package com.dylibso.chicory.fuzz;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.tools.wasm.WasmSmith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.apache.commons.lang3.RandomStringUtils;

public class WasmSmithWrapper {

    private static final Logger logger = new SystemLogger();

    private static final int BASE_SEED_SIZE = 1000;
    private String seed = getSeed(BASE_SEED_SIZE);

    @SuppressWarnings("deprecation")
    private static String getSeed(int size) {
        return RandomStringUtils.randomAlphabetic(size);
    }

    WasmSmithWrapper() {}

    public File run(String subfolder, String fileName, InstructionTypes instructionTypes)
            throws IOException {
        return run(subfolder, fileName, instructionTypes, "/smith.default.properties");
    }

    @SuppressWarnings("StringSplitter")
    public File run(
            String subfolder,
            String fileName,
            InstructionTypes instructionTypes,
            String smithProperties)
            throws IOException {
        var targetSubfolder = "target/fuzz/data/" + subfolder;
        var targetFolder = new File(targetSubfolder);
        targetFolder.mkdirs();
        var targetFile = new File(targetSubfolder + "/" + fileName);
        var seedFile = new File(targetSubfolder + "/seed.txt");

        // Parse properties
        var properties = new LinkedHashMap<String, String>();
        var propsFile =
                new String(getClass().getResourceAsStream(smithProperties).readAllBytes(), UTF_8);
        var props = propsFile.split("\n");
        for (var prop : props) {
            if (!prop.isEmpty()) {
                var split = prop.split("=");
                properties.put(split[0], split[1]);
            }
        }

        var retry = 5;
        var seedSize = BASE_SEED_SIZE;
        while (retry > 0) {
            // Write the seed file for reproducibility
            try (var outputStream = new FileOutputStream(seedFile)) {
                outputStream.write(seed.getBytes(UTF_8));
                outputStream.flush();
            }

            logger.info(
                    "Running wasm-smith with instructions="
                            + instructionTypes
                            + " seed-size="
                            + seed.length());

            try {
                var wasmBytes =
                        WasmSmith.run(
                                seed.getBytes(UTF_8), properties, instructionTypes.toString());

                try (var outputStream = new FileOutputStream(targetFile)) {
                    outputStream.write(wasmBytes);
                    outputStream.flush();
                }
                return targetFile;
            } catch (RuntimeException e) {
                logger.error("wasm-smith failed: " + e.getMessage());
                retry--;
            } finally {
                seed = getSeed(seedSize);
                seedSize = Math.min(seedSize * 2, 100_000);
            }
        }

        throw new IOException("wasm-smith failed after 5 retries");
    }
}
