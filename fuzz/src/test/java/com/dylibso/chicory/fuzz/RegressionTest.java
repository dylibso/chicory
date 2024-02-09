package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dylibso.chicory.runtime.HostImports;
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
        var instance = module.instantiate(new HostImports(), false);

        testModule(targetWasm, module, instance, false);
        // Sanity check that the starting function doesn't break
        assertDoesNotThrow(() -> module.instantiate());
    }
}
