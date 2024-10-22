package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.hostmodule.annotations.HostModule;
import com.dylibso.chicory.hostmodule.annotations.WasmExport;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WithImportsTest {
    public final AtomicInteger count = new AtomicInteger();

    @HostModule("console")
    class TestModule {
        private static final String EXPECTED = "Hello, World!";
        private final Instance instance;

        public TestModule() {
            instance =
                    Instance.builder(
                                    Parser.parse(
                                            WithImportsTest.class.getResourceAsStream(
                                                    "/compiled/host-function.wat.wasm")))
                            .withImportValues(
                                    new ImportValues(
                                            TestModule_ModuleFactory.toHostFunctions(this)))
                            .build();
        }

        public void logIt() {
            instance.export("logIt").apply();
        }

        @WasmExport
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
