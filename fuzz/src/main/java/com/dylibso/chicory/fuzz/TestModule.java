package com.dylibso.chicory.fuzz;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class TestModule {
    private static final Logger logger = new SystemLogger();
    WasmTimeWrapper wasmtime = new WasmTimeWrapper();
    ChicoryCliWrapper chicoryCli = new ChicoryCliWrapper();

    public List<String> paramsList(FunctionType type) {
        return type.params().stream().map(p -> randomNumber()).collect(Collectors.toList());
    }

    public String randomNumber() {
        // TODO: 2 digits integer seems not enough, but a starting point ...
        return RandomStringUtils.randomNumeric(2);
    }

    public List<TestResult> testModule(File targetWasm, Module module, Instance instance)
            throws Exception {
        return testModule(targetWasm, module, instance, true);
    }

    public List<TestResult> testModule(
            File targetWasm, Module module, Instance instance, boolean commitOnFailure)
            throws Exception {
        var results = new ArrayList<TestResult>();

        for (var i = 0; i < module.exportSection().exportCount(); i++) {
            var export = module.exportSection().getExport(i);
            switch (export.exportType()) {
                case FUNCTION:
                    {
                        logger.info("Going to test export " + export.name());
                        var typeId = export.index();
                        var type = instance.type(typeId);
                        var params = paramsList(type);

                        String oracleResult = null;
                        try {
                            oracleResult = wasmtime.run(targetWasm, export.name(), params);
                        } catch (IOException | RuntimeException e) {
                            // If the oracle failed we can skip ...
                            logger.error("Failed to run the oracle, skip the check on Chicory");
                            continue;
                        }

                        // Running Chicory on the command line to compare the results
                        String chicoryResult = null;
                        try {
                            chicoryResult = chicoryCli.run(targetWasm, export.name(), params);
                        } catch (IOException | RuntimeException e) {
                            logger.warn("Failed to run chicory, but wasmtime succeeded: " + e);
                        }
                        // To be used for files generation
                        var truncatedExportName =
                                export.name().substring(0, Math.min(export.name().length(), 32));

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
                        if (commitOnFailure && !oracleResult.equals(chicoryResult)) {
                            FileUtils.copyDirectory(
                                    targetWasm.getParentFile(),
                                    new File(
                                            "src/test/resources/crash-"
                                                    + targetWasm.getParentFile().getName()
                                                    + "-"
                                                    + truncatedExportName));
                        }

                        results.add(new TestResult(oracleResult, chicoryResult));
                        break;
                    }
                default:
                    // ignored for now
                    logger.info("Skipping export " + export.name());
                    break;
            }
        }

        return results;
    }
}
