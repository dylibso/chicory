package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.host.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ImportExportsTest {
    public final AtomicInteger count = new AtomicInteger();

    @WasmModuleInterface("host-function.wat.wasm")
    class TestModule implements TestModule_ModuleImports, TestModule_Console {
        private static final String EXPECTED = "Hello, World!";
        private final Instance instance;
        private final TestModule_ModuleExports exports;

        public TestModule() {
            var module =
                    Parser.parse(
                            ImportExportsTest.class.getResourceAsStream("/host-function.wat.wasm"));

            this.instance = Instance.builder(module).withImportValues(toImportValues()).build();
            this.exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }

        @Override
        public TestModule_Console console() {
            return this;
        }

        @Override
        public void log(int len, int offset) {
            var message = instance.memory().readString(offset, len);

            if (EXPECTED.equals(message)) {
                count.incrementAndGet();
            }
        }
    }

    @Test
    public void withImportsModule() {
        // Arrange
        var withImportsModule = new TestModule();

        // Act
        withImportsModule.exports().logIt();

        // Assert
        assertEquals(10, count.get());
    }
}
