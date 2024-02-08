package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.ExternalType;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;

public class FuzzTest {
    private static final Logger logger = new SystemLogger();

    WasmSmithWrapper smith = new WasmSmithWrapper();
    WasmTimeWrapper wasmtime = new WasmTimeWrapper();
    ChicoryCliWrapper chicoryCli = new ChicoryCliWrapper();

    File generateTestData(int num) throws Exception {
        var atLeastOneExportedFunction = false;

        var targetModuleName = "test.wasm";
        File targetWasm = null;
        while (!atLeastOneExportedFunction) {
            targetWasm =
                    smith.run(
                            "" + num,
                            targetModuleName,
                            new InstructionTypes(InstructionType.NUMERIC));

            atLeastOneExportedFunction =
                    Module.builder(targetWasm).build().exports().values().stream()
                            .filter(e -> e.exportType() == ExternalType.FUNCTION)
                            .findAny()
                            .isPresent();
        }

        return targetWasm;
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        int currentRepetition = repetitionInfo.getCurrentRepetition();

        int totalRepetitions = repetitionInfo.getTotalRepetitions();
        String methodName = testInfo.getTestMethod().get().getName();
        logger.info(
                String.format(
                        "About to execute repetition %d of %d for %s", //
                        currentRepetition, totalRepetitions, methodName));
    }

    @AfterEach
    void afterEach(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        // TODO copy failing test folders in `resources` and add a test for those failures
        // TODO print the seed to std out to be able to reproduce issues found in CI
    }

    void testModule(File targetWasm, Module module, Instance instance) throws Exception {
        for (var export : module.exports().entrySet()) {
            switch (export.getValue().exportType()) {
                case FUNCTION:
                    {
                        logger.info("Going to test export " + export.getKey());
                        var exportSig = module.export(export.getKey());
                        var typeId = instance.functionType(exportSig.index());
                        var type = instance.type(typeId);
                        // TODO: we can pass more interesting arguments when needed
                        var params =
                                Arrays.stream(type.params())
                                        .map(p -> RandomStringUtils.randomNumeric(2))
                                        .collect(Collectors.toList());

                        String oracleResult = null;
                        try {
                            oracleResult = wasmtime.run(targetWasm, export.getKey(), params);
                        } catch (Exception e) {
                            // If the oracle failed we can skip ...
                            logger.error("Failed to run the oracle, skip the check on Chicory");
                            continue;
                        }
                        logger.warn("Oracle Result: " + oracleResult);

                        // Running Chicory on the command line to compare the results
                        String chicoryResult = null;
                        try {
                            chicoryResult = chicoryCli.run(targetWasm, export.getKey(), params);
                        } catch (Exception e) {
                            logger.warn("Failed to run chicory, but wasmtime succeeded: " + e);
                        }
                        logger.warn("Chicory Result: " + chicoryResult);

                        System.err.println("\u001B[31mOracle:\n" + oracleResult + "\u001B[0m");
                        System.err.println("\u001B[31mChicory:\n" + chicoryResult + "\u001B[0m");
                        try (var outputStream =
                                new FileOutputStream(
                                        targetWasm.getParentFile()
                                                + "/result-"
                                                + export.getKey()
                                                + ".txt")) {
                            outputStream.write(
                                    ("Oracle:\n" + oracleResult + "\nChicory:\n" + chicoryResult)
                                            .getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
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

    @RepeatedTest(1000)
    public void basicFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm = generateTestData(repetitionInfo.getCurrentRepetition());
        var module = Module.builder(targetWasm).build();
        var instance = module.instantiate(new HostImports(), false);

        testModule(targetWasm, module, instance);
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }
}
