package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class TestModule {

    private static final Logger logger = new SystemLogger();

    WasmTimeWrapper wasmtime = new WasmTimeWrapper();
    ChicoryCliWrapper chicoryCli = new ChicoryCliWrapper();

    public String randomNumber() {
        // TODO: 2 digits integer seems not enough, but a starting point ...
        return RandomStringUtils.randomNumeric(2);
    }

    public void testModule(File targetWasm, Module module, Instance instance) throws Exception {
        for (var export : module.exports().entrySet()) {
            switch (export.getValue().exportType()) {
                case FUNCTION:
                    {
                        logger.info("Going to test export " + export.getKey());
                        var exportSig = module.export(export.getKey());
                        var typeId = instance.functionType(exportSig.index());
                        var type = instance.type(typeId);

                        var params =
                                Arrays.stream(type.params())
                                        .map(p -> randomNumber())
                                        .collect(Collectors.toList());

                        String oracleResult = null;
                        try {
                            oracleResult = wasmtime.run(targetWasm, export.getKey(), params);
                        } catch (Exception e) {
                            // If the oracle failed we can skip ...
                            logger.error("Failed to run the oracle, skip the check on Chicory");
                            continue;
                        }

                        // Running Chicory on the command line to compare the results
                        String chicoryResult = null;
                        try {
                            chicoryResult = chicoryCli.run(targetWasm, export.getKey(), params);
                        } catch (Exception e) {
                            logger.warn("Failed to run chicory, but wasmtime succeeded: " + e);
                        }
                        // To be used for files generation
                        var truncatedExportName =
                                export.getKey()
                                        .substring(0, Math.min(export.getKey().length(), 32));

                        if (!oracleResult.isEmpty() || !chicoryResult.isEmpty()) {
                            System.err.println("\u001B[31mOracle:\n" + oracleResult + "\u001B[0m");
                            System.err.println(
                                    "\u001B[31mChicory:\n" + chicoryResult + "\u001B[0m");
                            try (var outputStream =
                                    new FileOutputStream(
                                            targetWasm.getParentFile()
                                                    + "/result-"
                                                    + truncatedExportName
                                                    + ".txt")) {
                                outputStream.write(
                                        ("Oracle:\n"
                                                        + oracleResult
                                                        + "\nChicory:\n"
                                                        + chicoryResult)
                                                .getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            }
                        }
                        // The test is going to fail, copy folders
                        if (!oracleResult.equals(chicoryResult)) {
                            FileUtils.copyDirectory(
                                    targetWasm.getParentFile(),
                                    new File(
                                            "src/test/resources/crash-"
                                                    + targetWasm.getParentFile().getName()
                                                    + "-"
                                                    + truncatedExportName));
                        }

                        assertEquals(oracleResult, chicoryResult);
                        break;
                    }
                default:
                    // ignored for now
                    logger.info("Skipping export " + export.getKey());
                    break;
            }
        }
    }
}
