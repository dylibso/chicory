package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;

public class WasmSmithWrapper {

    private static final Logger logger = new SystemLogger();

    public static final List<String> BINARY_NAME = List.of("wasm-tools", "smith");

    private int BASE_SEED_SIZE = 100;
    private String seed = getSeed(BASE_SEED_SIZE);

    // A smaller size of the seed speeds up the execution
    private static String getSeed(int size) {
        return Optional.ofNullable(System.getenv("CHICORY_FUZZ_SEED"))
                .orElseGet(() -> RandomStringUtils.randomAlphabetic(size));
    }

    WasmSmithWrapper() {}

    public File run(String subfolder, String fileName, InstructionTypes instructionTypes)
            throws Exception {
        var targetFolder = new File("target/fuzz/data/" + subfolder);
        targetFolder.mkdirs();
        var targetFile = new File("target/fuzz/data/" + subfolder + "/" + fileName);
        var seedFile = new File("target/fuzz/data/" + subfolder + "/seed.txt");

        var command = new ArrayList<>(BINARY_NAME);
        // breaks the execution
        // "--ensure-termination",
        // "true"
        // TODO: make this configurable on a test-by-test basis
        var defaultProperties = new ArrayList<String>();
        var propsFile =
                new String(
                        getClass().getResourceAsStream("/smith.default.properties").readAllBytes(),
                        StandardCharsets.UTF_8);
        var props = propsFile.split("\n");
        for (var prop : props) {
            if (!prop.isEmpty()) {
                var split = prop.split("=");
                defaultProperties.add("--" + split[0]);
                defaultProperties.add(split[1]);
            }
        }
        command.addAll(defaultProperties);
        command.addAll(
                List.of(
                        "--allowed-instructions",
                        instructionTypes.toString(),
                        "--output",
                        targetFile.getAbsolutePath()));
        logger.info(
                "Going to execute command:\n"
                        + String.join(" ", command)
                        + " < "
                        + seedFile.getAbsolutePath());

        var retry = 3;
        var seedSize = BASE_SEED_SIZE;
        while (retry > 0) {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("."));

            // write the seed file
            try (var outputStream = new FileOutputStream(seedFile)) {
                outputStream.write((seed).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            pb.redirectInput(seedFile);
            Process ps;
            try {
                ps = pb.start();
                ps.waitFor(10, TimeUnit.SECONDS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                // Renew the seed
                seed = getSeed(seedSize);
                seedSize = seedSize * 10;
            }

            if (ps.exitValue() != 0) {
                logger.error("wasm-smith exiting with:" + ps.exitValue());
                logger.error(new String(ps.getErrorStream().readAllBytes()));
                retry--;
            } else {
                break;
            }
        }

        return targetFile;
    }
}
