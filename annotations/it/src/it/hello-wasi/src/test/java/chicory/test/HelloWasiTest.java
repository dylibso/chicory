package chicory.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.host.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class HelloWasiTest {

    @WasmModuleInterface("hello-wasi.wat.wasm")
    class TestModule implements TestModule_ModuleImports, TestModule_WasiSnapshotPreview1 {
        private final Instance instance;
        private final TestModule_ModuleExports exports;
        private final WasiPreview1 wasi;
        public final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public TestModule() {
            wasi =
                    WasiPreview1.builder()
                            .withOptions(WasiOptions.builder().withStdout(baos).build())
                            .build();
            var module =
                    Parser.parse(HelloWasiTest.class.getResourceAsStream("/hello-wasi.wat.wasm"));

            this.instance =
                    Instance.builder(module)
                            .withImportValues(toImportValues())
                            .withStart(false) // Needed to avoid circular references of instance
                            .build();

            this.exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }

        @Override
        public TestModule_WasiSnapshotPreview1 wasiSnapshotPreview1() {
            return this;
        }

        @Override
        public int fdWrite(int fd, int iovs, int iovsLen, int nwrittenPtr) {
            return wasi.fdWrite(instance.memory(), fd, iovs, iovsLen, nwrittenPtr);
        }
    }

    @Test
    public void helloWasiModule() {
        // Arrange
        var helloWasiModule = new TestModule();

        // Act
        helloWasiModule.exports()._start();

        // Assert
        assertEquals("hello world\n", helloWasiModule.baos.toString(UTF_8));
    }
}
