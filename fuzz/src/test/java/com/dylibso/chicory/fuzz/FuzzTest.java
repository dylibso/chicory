package com.dylibso.chicory.fuzz;

import static com.dylibso.chicory.fuzz.RepeatedTestConfig.FUZZ_TEST_NUMERIC;
import static com.dylibso.chicory.fuzz.RepeatedTestConfig.FUZZ_TEST_TABLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;

public class FuzzTest extends TestModule {
    private static final Logger logger = new SystemLogger();
    WasmSmithWrapper smith = new WasmSmithWrapper();

    File generateTestData(String prefix, int num, InstructionType... instructionTypes)
            throws Exception {
        var atLeastOneExportedFunction = false;

        File targetWasm = null;
        while (!atLeastOneExportedFunction) {
            targetWasm =
                    smith.run(prefix + num, "test.wasm", new InstructionTypes(instructionTypes));

            var exportSection = WasmModule.builder(targetWasm).build().exportSection();
            atLeastOneExportedFunction = false;
            for (int i = 0; i < exportSection.exportCount(); i++) {
                if (exportSection.getExport(i).exportType() == ExternalType.FUNCTION) {
                    atLeastOneExportedFunction = true;
                    break;
                }
            }
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

    @RepeatedTest(value = FUZZ_TEST_NUMERIC, failureThreshold = 1) // stop on first failure
    public void numericOnlyFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm =
                generateTestData(
                        "numeric-", repetitionInfo.getCurrentRepetition(), InstructionType.NUMERIC);
        var module = WasmModule.builder(targetWasm).build();
        var instance = Instance.builder(module).withInitialize(true).withStart(false).build();

        var results = testModule(targetWasm, module, instance);

        for (var res : results) {
            assertEquals(res.getOracleResult(), res.getChicoryResult());
        }
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> Instance.builder(module).build());
    }

    @RepeatedTest(value = FUZZ_TEST_TABLE, failureThreshold = 1) // stop on first failure
    public void tableOnlyFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm =
                generateTestData(
                        "table-", repetitionInfo.getCurrentRepetition(), InstructionType.TABLE);
        var module = WasmModule.builder(targetWasm).build();
        var instance = Instance.builder(module).withInitialize(true).withStart(false).build();

        var results = testModule(targetWasm, module, instance);

        for (var res : results) {
            assertEquals(res.getOracleResult(), res.getChicoryResult());
        }
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> Instance.builder(module).build());
    }
}
