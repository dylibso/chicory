package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class CollidingExportsTest {

    @WasmModuleInterface("colliding-exports.wat.wasm")
    class TestModule {
        private final TestModule_ModuleExports exports;
        private final WasiPreview1 wasi;
        public final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public TestModule() {
            wasi =
                    WasiPreview1.builder()
                            .withOptions(WasiOptions.builder().withStdout(baos).build())
                            .build();
            var module =
                    Parser.parse(
                            CollidingExportsTest.class.getResourceAsStream(
                                    "/colliding-exports.wat.wasm"));

            var instance = Instance.builder(module).build();
            exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }
    }

    @Test
    public void collidingImportsModule() {
        // Arrange
        var module = new TestModule();

        // Act
        var add = module.exports().func(1, 3);
        var mul = module.exports()._func(2, 3);
        var sub = module.exports().Func(2, 1);

        // Assert
        assertEquals(4, add);
        assertEquals(6, mul);
        assertEquals(1, sub);
    }
}
