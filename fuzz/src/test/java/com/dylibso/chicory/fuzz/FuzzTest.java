package com.dylibso.chicory.fuzz;

import static com.dylibso.chicory.fuzz.RepeatedTestConfig.FUZZ_TEST_NUMERIC;
import static com.dylibso.chicory.fuzz.RepeatedTestConfig.FUZZ_TEST_TABLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
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

    @RepeatedTest(value = FUZZ_TEST_NUMERIC, failureThreshold = 1) // stop on first failure
    public void numericOnlyFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm =
                generateTestData(
                        "numeric-", repetitionInfo.getCurrentRepetition(), InstructionType.NUMERIC);
        var module = Module.builder(targetWasm).build();
        var instance = module.instantiate(new HostImports(), false);

        testModule(targetWasm, module, instance);
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }

    @RepeatedTest(value = FUZZ_TEST_TABLE, failureThreshold = 1) // stop on first failure
    public void tableOnlyFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm =
                generateTestData(
                        "table-", repetitionInfo.getCurrentRepetition(), InstructionType.TABLE);
        var module = Module.builder(targetWasm).build();
        var instance = module.instantiate(new HostImports(), false);

        testModule(targetWasm, module, instance);
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }
}
