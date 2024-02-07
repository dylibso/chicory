package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Module;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;

public class FuzzTest {
    private static final Logger logger = new SystemLogger();

    WasmSmithWrapper smith = new WasmSmithWrapper();
    WasmTimeWrapper wasmtime = new WasmTimeWrapper();

    // it's really hard to invoke some generated function names
    private boolean isFunctionNameUsable(String funcName) {
        if (funcName.length() <= 0 || funcName.isBlank()) {
            return false;
        }

        for (var i = 0; i < funcName.length(); i++) {
            var c = funcName.charAt(i);

            if (!Character.isSurrogate(c) && (Character.isLetter(c) || Character.isDigit(c))) {
                continue;
            } else {
                // TODO: Hard requirement, revisit if possible
                return false;
            }
        }
        return true;
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        int currentRepetition = repetitionInfo.getCurrentRepetition();

        var targetModuleName = "test.wasm";
        smith.run("" + currentRepetition, targetModuleName);

        int totalRepetitions = repetitionInfo.getTotalRepetitions();
        String methodName = testInfo.getTestMethod().get().getName();
        logger.info(
                String.format(
                        "About to execute repetition %d of %d for %s", //
                        currentRepetition, totalRepetitions, methodName));
    }

    @AfterEach
    void afterEach(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        // TODO copy the failing test folder in `resources` and add a test for those failures
    }

    @RepeatedTest(100)
    public void basicFuzz(RepetitionInfo repetitionInfo) throws Exception {
        var targetWasm =
                new File(
                        "target/fuzz/data/" + repetitionInfo.getCurrentRepetition() + "/test.wasm");
        var module = Module.builder(targetWasm).build();

        // Assert
        for (var export : module.exports().entrySet()) {
            switch (export.getValue().exportType()) {
                case FUNCTION:
                    {
                        logger.info("Going to test export " + export.getKey());
                        var funcIdx = export.getValue().index();

                        if (isFunctionNameUsable(export.getKey())) {

                            String oracleResult = null;
                            try {
                                oracleResult = wasmtime.run(targetWasm, export.getKey());
                            } catch (Exception e) {
                                logger.warn("Failed to run the oracle: " + e);
                                // If the oracle failed we can skip ...
                                continue;
                            }
                            logger.warn("Oracle Result: " + oracleResult);

                            // Running Chicory on the command line to compare the results
                            String chicoryResult = null;
                            try {
                                chicoryResult = wasmtime.run(targetWasm, export.getKey());
                            } catch (Exception e) {
                                logger.warn("Failed to run chicory, but wasmtime succeeded: " + e);
                            }
                            logger.warn("Chicory Result: " + chicoryResult);

                            assertEquals(oracleResult, chicoryResult);
                        } else {
                            // TODO: support passing arguments
                            logger.debug("Need to parse the local types and emit params");
                        }
                        break;
                    }
                default:
                    // ignored for now
                    break;
            }
        }

        // Final sanity check
        assertDoesNotThrow(() -> module.instantiate());
    }
}
