package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.Value;
import org.junit.jupiter.api.Test;

class AllExportsTest {

    @WasmModuleInterface("all-exports.wat.wasm")
    class TestModule {
        private final TestModule_ModuleExports exports;

        public TestModule() {
            super();
            var module =
                    Parser.parse(AllExportsTest.class.getResourceAsStream("/all-exports.wat.wasm"));

            var instance = Instance.builder(module).build();
            this.exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }
    }

    @Test
    public void exportsModule() {
        // Arrange
        var exportsModule = new TestModule().exports();

        // Assert
        assertEquals(exportsModule.glob1().getValue(), exportsModule.get1());
        assertEquals(exportsModule.glob2().getValue(), exportsModule.get2());
        assertEquals(Value.longToFloat(exportsModule.glob3().getValue()), exportsModule.get3());
        assertEquals(Value.longToDouble(exportsModule.glob4().getValue()), exportsModule.get4());
        assertEquals(exportsModule.glob1().getValue() + 5, exportsModule.get5(5));
        assertEquals(exportsModule.glob2().getValue() + 6, exportsModule.get6(6));
        assertEquals(
                Value.longToFloat(exportsModule.glob3().getValue()) + 7.0f,
                exportsModule.get7(7.0f));
        assertEquals(
                Value.longToDouble(exportsModule.glob4().getValue()) + 8.0d,
                exportsModule.get8(8.0d));
        assertEquals(46, exportsModule.get9(1, 2L, 3.0f, 4.0d));
        var multiReturn = exportsModule.get10(1, 2L, 3.0f, 4.0d);
        assertEquals(47, multiReturn[0]);
        assertEquals(48, multiReturn[1]);
        assertNotNull(exportsModule.mem());
        assertNotNull(exportsModule.tab());
    }
}
