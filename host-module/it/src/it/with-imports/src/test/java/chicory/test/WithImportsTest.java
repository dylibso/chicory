package chicory.test;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WithImportsTest {
    public final AtomicInteger count = new AtomicInteger();

    @WasmModuleInterface("host-function.wat.wasm")
    class TestModule {}

    //    @HostModule("console")
    //    class TestModule {
    //        private static final String EXPECTED = "Hello, World!";
    //        private final Instance instance;
    //
    //        public TestModule() {
    //            instance =
    //                    Instance.builder(
    //                                    Parser.parse(
    //                                            WithImportsTest.class.getResourceAsStream(
    //                                                    "/compiled/host-function.wat.wasm")))
    //                            .withImportValues(
    //                                    ImportValues.builder()
    //                                            .addFunction(
    //
    // TestModule_ModuleFactory.toHostFunctions(this))
    //                                            .build())
    //                            .build();
    //        }
    //
    //        public void logIt() {
    //            instance.export("logIt").apply();
    //        }
    //
    //        @WasmExport
    //        public void log(int len, int offset) {
    //            var message = instance.memory().readString(offset, len);
    //
    //            if (EXPECTED.equals(message)) {
    //                count.incrementAndGet();
    //            }
    //        }
    //    }

    @Test
    public void withImportsModule() {
        // Arrange
        var withImportsModule = new TestModule();

        // Act
        //        withImportsModule.logIt();

        // Assert
        //        assertEquals(10, count.get());
    }
}
