package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.runtime.Module;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RegressionTest extends TestModule {

    private static Stream<Arguments> crashFolders() {
        return Arrays.stream(new File("src/test/resources").listFiles())
                .filter(f -> f.isDirectory() && f.getName().startsWith("crash"))
                .map(d -> Arguments.of(d));
    }

    @ParameterizedTest
    @MethodSource("crashFolders")
    void regressionTests(File folder) throws Exception {
        var targetWasm = new File(folder.getAbsolutePath() + "/test.wasm");
        var module = Module.builder(targetWasm).build();
        var instance = module.withInitialize(true).withStart(false).instantiate();

        var results = testModule(targetWasm, module, instance, false);

        for (var res : results) {
            assertEquals(res.getOracleResult(), res.getChicoryResult());
        }
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }
}
