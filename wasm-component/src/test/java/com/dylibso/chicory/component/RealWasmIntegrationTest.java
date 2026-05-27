package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
@DisplayName("Real Wasm Integration")
class RealWasmIntegrationTest {

    @AfterEach
    void clearRealloc() {
        CanonicalAbi.clearRealloc();
    }

    @Nested
    @DisplayName("Module Invocation")
    class ModuleInvocationTests {
        @Test
        @DisplayName("calls a real add export")
        void callsARealAddExport() {
            WasmModule module = loadModule("compiled/add.wat.wasm");
            Instance instance = new Store().instantiate("add_module", module);
            ExportFunction addFunction = instance.export("add");

            assertNotNull(module);
            assertNotNull(instance);
            assertNotNull(addFunction);
            assertEquals(8, addFunction.apply(3, 5)[0]);
        }

        @Test
        @DisplayName("parses component signatures for a real module")
        void parsesComponentSignaturesForARealModule() {
            WasmModule module = loadModule("compiled/add.wat.wasm");
            Instance instance = new Store().instantiate("add_module", module);
            ComponentDefinition definition =
                    new WitParser().parse("export add: function(a: i32, b: i32) -> i32\n");

            assertNotNull(instance);
            assertEquals(1, definition.exports().size());
            assertEquals("add", definition.exports().get(0).name());
            assertEquals(30, instance.export("add").apply(10, 20)[0]);
        }

        @Test
        @DisplayName("creates instances even when memory is not exported")
        void createsInstancesEvenWhenMemoryIsNotExported() {
            Instance instance =
                    new Store().instantiate("add_module", loadModule("compiled/add.wat.wasm"));
            assertNotNull(instance);
        }
    }

    @Nested
    @DisplayName("Canonical ABI")
    class CanonicalAbiTests {
        @Test
        @DisplayName("encodes and decodes basic integer values")
        void encodesAndDecodesBasicIntegerValues() {
            long[] encoded = CanonicalAbi.encode(42, PrimitiveType.I32, null);
            assertEquals(42, encoded[0]);
            assertEquals(42, CanonicalAbi.decode(encoded, PrimitiveType.I32, null));
        }

        @Test
        @DisplayName("encodes strings without realloc using a null pointer")
        void encodesStringsWithoutReallocUsingANullPointer() {
            long[] encoded = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);
            assertEquals(2, encoded.length);
            assertEquals(0, encoded[0]);
            assertEquals(5, encoded[1]);
        }

        @Test
        @DisplayName("encodes strings with realloc using allocated memory")
        void encodesStringsWithReallocUsingAllocatedMemory() {
            CanonicalAbi.withRealloc(new MockCanonicalRealloc());
            long[] encoded = CanonicalAbi.encode("test", PrimitiveType.STRING, null);

            assertEquals(1024, encoded[0]);
            assertEquals(4, encoded[1]);
        }
    }

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    private static final class MockCanonicalRealloc implements ExportFunction {
        private int allocatedPtr = 1024;

        @Override
        public long[] apply(long... args) {
            int result = allocatedPtr;
            allocatedPtr += (int) args[3];
            return new long[] {result};
        }
    }
}
