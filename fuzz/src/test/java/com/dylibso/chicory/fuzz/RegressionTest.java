package com.dylibso.chicory.fuzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RegressionTest extends TestModule {

    private final WasmRunner interpreterRunner = new ChicoryRunner();
    private final WasmRunner compilerRunner = new ChicoryRunner(MachineFactoryCompiler::compile);

    private static Stream<Arguments> crashFolders() {
        return Stream.of(new File("src/test/resources"), new File("target/crash-reproducers"))
                .filter(File::isDirectory)
                .flatMap(
                        dir -> {
                            var dirs = dir.listFiles();
                            return dirs != null ? Arrays.stream(dirs) : Stream.<File>empty();
                        })
                .filter(f -> f.isDirectory() && f.getName().startsWith("crash"))
                .map(d -> Arguments.of(d));
    }

    @ParameterizedTest
    @MethodSource("crashFolders")
    void regressionTests(File folder) throws Exception {
        var targetWasm = new File(folder.getAbsolutePath() + "/test.wasm");

        // Check if this is a parse failure reproducer
        var propsFile = new File(folder, "crash-info.properties");
        if (propsFile.exists()) {
            var props = new Properties();
            try (var in = new FileInputStream(propsFile)) {
                props.load(in);
            }
            var phase = props.getProperty("functionName");
            if ("parse".equals(phase)) {
                // Parse failure reproducer — verify the parser still rejects it
                assertThrows(RuntimeException.class, () -> Parser.parse(targetWasm));
                return;
            }
            if ("instantiate".equals(phase)) {
                // Instantiation failure reproducer — verify it still fails
                var mod = Parser.parse(targetWasm);
                assertThrows(
                        RuntimeException.class,
                        () -> Instance.builder(mod).withInitialize(true).withStart(false).build());
                return;
            }
        }

        // Runtime failure reproducer — run the differential comparison
        var module = Parser.parse(targetWasm);
        var instance = Instance.builder(module).withInitialize(true).withStart(false).build();

        var results =
                testModule(
                        targetWasm,
                        module,
                        instance,
                        interpreterRunner,
                        compilerRunner,
                        "regression",
                        false);

        for (var res : results) {
            if (res.getChicoryResult() != null) {
                assertEquals(res.getOracleResult(), res.getChicoryResult());
            }
        }
        assertDoesNotThrow(() -> Instance.builder(module).build());
    }
}
