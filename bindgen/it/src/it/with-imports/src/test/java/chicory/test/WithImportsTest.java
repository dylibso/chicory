package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.gen.Module;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WithImportsTest {
    public final AtomicInteger count = new AtomicInteger();

    @HostModule("console")
    class TestModule extends Module {
        private final Instance instance;
        private static final String EXPECTED = "Hello, World!";

        public TestModule() {
            instance =
                    Instance.builder(
                                    Parser.parse(
                                            WithImportsTest.class.getResourceAsStream(
                                                    "/compiled/host-function.wat.wasm")))
                            .withHostImports(
                                    new HostImports(TestModule_ModuleFactory.toHostFunctions(this)))
                            .build();
        }

        @Override
        public Instance instance() {
            return this.instance;
        }

        @WasmExport
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
        withImportsModule.logIt();

        // Assert
        assertEquals(10, count.get());
    }
}
