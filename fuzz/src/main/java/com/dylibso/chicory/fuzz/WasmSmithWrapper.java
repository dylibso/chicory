package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WasmSmithWrapper {

    private static final Logger logger = new SystemLogger();

    public static final List<String> BINARY_NAME = List.of("wasm-tools", "smith");
    private String seed = getSeed();

    private static String getSeed() {
        return Optional.ofNullable(System.getenv("CHICORY_FUZZ_SEED"))
                .orElseGet(
                        () -> {
                            byte[] array = new byte[1000]; // 100 fixed bytes?
                            new Random().nextBytes(array);
                            return new String(array, Charset.forName("UTF-8"));
                        });
    }

    WasmSmithWrapper() {}

    public File run(String fileName) throws Exception {
        // Those things should be externally tunable
        var instructionTypes = new InstructionTypes(InstructionType.NUMERIC);
        var targetFolder = new File("target/fuzz/data");
        targetFolder.mkdirs();
        var targetFile = new File("target/fuzz/data/" + fileName);
        var lastSeedFile = new File("target/fuzz/last_seed.txt");

        var command = new ArrayList<>(BINARY_NAME);
        // breaks the execution
        // "--ensure-termination",
        // "true"
        command.addAll(
                List.of(
                        "--min-exports", // trying to generate something to compare with
                        "1",
                        "--max-imports", // TODO: for now... let see how to handle this
                        "0",
                        "--max-modules", // TODO: Support for multi-modules
                        "1",
                        "--allowed-instructions",
                        instructionTypes.toString(),
                        "--output",
                        targetFile.getAbsolutePath()));
        logger.info(
                "Going to execute command:\n"
                        + String.join(" ", command)
                        + " < "
                        + lastSeedFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("."));

        // write the seed file
        if (lastSeedFile.exists()) {
            lastSeedFile.delete();
        }
        try (var outputStream = new FileOutputStream(lastSeedFile)) {
            outputStream.write((seed).getBytes(StandardCharsets.UTF_8));
        }

        pb.redirectInput(lastSeedFile);
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
            seed = getSeed();
        }

        if (ps.exitValue() != 0) {
            System.err.println("wasm-smith exiting with:" + ps.exitValue());
            System.err.println(ps.getErrorStream().toString());
            throw new RuntimeException("Failed to execute wasm-smith program.");
        }

        return targetFile;
    }
}
