package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Module;
import org.junit.jupiter.api.Test;

public class FuzzTest {
    private static final Logger logger = new SystemLogger();

    WasmSmithWrapper smith = new WasmSmithWrapper();
    WasmTimeWrapper wasmtime = new WasmTimeWrapper();

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

    @Test
    public void basicFuzz() throws Exception {
        var functionInvoked = false;

        while (!functionInvoked) {
            // Arrange
            var targetModuleName = "test.0.wasm";
            var targetFile = smith.run(targetModuleName);
            var module = Module.builder(targetFile).build();

            // Act
            var instance = assertDoesNotThrow(() -> module.instantiate());
            // var oracleResult = wasmtime.run(targetFile);

            // Assert
            for (var export : module.exports().entrySet()) {
                switch (export.getValue().exportType()) {
                    case FUNCTION:
                        {
                            logger.info("Going to test export " + export.getKey());
                            var funcIdx = export.getValue().index();
                            var func = instance.function(funcIdx);

                            if (func.localTypes().size() == 0
                                    && isFunctionNameUsable(export.getKey())) {

                                String oracleResult = null;
                                try {
                                    oracleResult = wasmtime.run(targetFile, export.getKey());
                                } catch (Exception e) {
                                    logger.warn("Failed to run the oracle: " + e);
                                    continue;
                                }
                                logger.warn("Oracle Result: " + oracleResult);

                                var chicoryResult = instance.export(export.getKey()).apply();
                                logger.warn("Chicory Result: " + chicoryResult);

                                var chicoryStringResult =
                                        (chicoryResult == null) ? "" : chicoryResult;
                                assertEquals(oracleResult, chicoryStringResult);

                                functionInvoked = true;
                            } else {
                                // TODO: support passing arguments
                                logger.debug(
                                        "Need to parse the local types and emit params "
                                                + func.localTypes().size());
                            }
                            break;
                        }
                    default:
                        // ignored for now
                        break;
                }
            }
        }
    }
}
