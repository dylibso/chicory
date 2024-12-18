package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

class ExportsTest {

    @WasmModuleInterface("exports.wat.wasm")
    class TestModule implements TestModule_ModuleExports {
        private final Instance instance;

        public TestModule() {
            var module =
                    Parser.parse(
                            ExportsTest.class.getResourceAsStream("/compiled/exports.wat.wasm"));

            instance = Instance.builder(module).build();
        }

        @Override
        public Instance instance() {
            return instance;
        }
    }

    @Test
    public void exportsModule() {
        // Arrange
        var exportsModule = new TestModule();

        // Assert
        assertEquals(exportsModule.glob1().getValue(), exportsModule.get1());
        assertEquals(exportsModule.glob2().getValue(), exportsModule.get2());
        assertEquals(exportsModule.glob3().getValue(), exportsModule.get3());
        assertEquals(exportsModule.glob4().getValue(), exportsModule.get4());
        assertEquals(exportsModule.mem(), exportsModule.instance().memory());
        assertNotNull(exportsModule.tab());
    }
}
