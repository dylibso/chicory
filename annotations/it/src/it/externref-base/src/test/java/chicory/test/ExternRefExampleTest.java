package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

class ExternRefExampleTest {

    @WasmModuleInterface("externref-example.wat.wasm")
    class TestModule implements TestModule_ModuleImports, TestModule_Env {
        private final Instance instance;

        private final TestModule_ModuleExports exports;

        public TestModule() {
            var module =
                    Parser.parse(
                            ExternRefExampleTest.class.getResourceAsStream(
                                    "/compiled/externref-example.wat.wasm"));
            this.instance = Instance.builder(module).withImportValues(toImportValues()).build();
            this.exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_Env env() {
            return this;
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }

        private Object sampleObj = null;

        public long getHostObject() {
            sampleObj = new Object();
            return 123;
        }

        public int isNull(long arg0) {
            if (arg0 != 123) {
                throw new RuntimeException("unrecognized external ref");
            }
            return (sampleObj == null) ? 1 : 0;
        }
    }

    @Test
    public void testExternRef() {
        var module = new TestModule();

        assertEquals(1, module.exports().isNull(123L));

        var hostObj = module.exports().getHostObject();
        assertEquals(123, hostObj);

        assertEquals(0, module.exports().isNull(123L));
    }
}
