package chicory.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MixedImportsTest {

    @WasmModuleInterface("mixed-imports.wat.wasm")
    class TestModule implements TestModule_ModuleImports, TestModule_Env {
        public final AtomicReference logResult = new AtomicReference<String>(null);
        private final Instance instance;
        private final TestModule_ModuleExports exports;
        private final Memory memory;

        public TestModule() {
            memory = new ByteBufferMemory(new MemoryLimits(1, 2));
            var module =
                    Parser.parse(
                            MixedImportsTest.class.getResourceAsStream("/mixed-imports.wat.wasm"));

            this.instance = Instance.builder(module).withImportValues(toImportValues()).build();
            this.exports = new TestModule_ModuleExports(instance);
        }

        public TestModule_ModuleExports exports() {
            return exports;
        }

        @Override
        public TestModule_Env env() {
            return this;
        }

        @Override
        public Memory memory() {
            return memory;
        }

        @Override
        public double cbrt(int arg0) {
            return Math.cbrt(arg0);
        }

        @Override
        public void log(int logLevel, double value) {
            logResult.set(logLevel + ": " + value);
        }
    }

    @Test
    public void importsModule() {
        // Arrange
        var importsModule = new TestModule();

        // Act
        importsModule.exports().main();

        // Assert
        assertEquals("1: 164.0", importsModule.logResult.get());
    }
}
