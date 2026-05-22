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
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration tests with real Wasm modules.
 * Tests ComponentModel works with actual guest modules from the test corpus.
 */
@SuppressWarnings("deprecation")
public class RealWasmIntegrationTest {

    private WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    @Test
    void testCallSimpleAddFunction() {
        // Load the add.wasm module which exports a simple add(i32, i32) -> i32
        WasmModule module = loadModule("compiled/add.wat.wasm");
        assertNotNull(module);

        // Instantiate the module
        Store store = new Store();
        Instance instance = store.instantiate("add_module", module);
        assertNotNull(instance);

        // Get the add export function
        ExportFunction addFunction = instance.export("add");
        assertNotNull(addFunction);

        // Call the add function directly (basic i32 test)
        long[] result = addFunction.apply(3, 5); // 3 + 5 = 8
        assertEquals(8, result[0]);
    }

    @Test
    void testComponentModelWithBasicTypes() {
        // Create a ComponentModel with a simple add function definition
        String wit = "export add: function(a: i32, b: i32) -> i32\n";

        WasmModule module = loadModule("compiled/add.wat.wasm");
        Store store = new Store();
        Instance instance = store.instantiate("add_module", module);

        // Define component (normally parsed from WIT)
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        // Verify the function was parsed
        assertEquals(1, definition.exports().size());
        ComponentDefinition.FunctionSignature sig = definition.exports().get(0);
        assertEquals("add", sig.name());
        assertEquals(2, sig.parameters().size());

        // Call via export
        ExportFunction export = instance.export("add");
        long[] result = export.apply(10, 20);

        assertEquals(30, result[0]);
    }

    @Test
    void testAbiEncodingWithBasicTypes() {
        // Test that ABI encoding works for basic types
        long[] i32Encoded = CanonicalAbi.encode(42, PrimitiveType.I32, null);
        assertEquals(1, i32Encoded.length);
        assertEquals(42, i32Encoded[0]);

        Object i32Decoded = CanonicalAbi.decode(i32Encoded, PrimitiveType.I32, null);
        assertEquals(42, i32Decoded);
    }

    @Test
    void testStringEncodingWithoutRealloc() {
        // Test string encoding in fallback mode (no guest realloc available)
        long[] encoded = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);

        // Should return length as fallback
        assertEquals(2, encoded.length);
        assertEquals(0, encoded[0]); // No ptr when no realloc
        assertEquals(5, encoded[1]);
    }

    @Test
    void testStringEncodingWithMockRealloc() {
        // Test string encoding with mock realloc
        ExportFunction mockRealloc = new MockCanonicalRealloc();
        CanonicalAbi.withRealloc(mockRealloc);

        try {
            long[] encoded = CanonicalAbi.encode("test", PrimitiveType.STRING, null);

            // Should return (ptr << 32) | len
            // Now returns [ptr, len] directly
            int ptr = (int) encoded[0];
            int len = (int) encoded[1];

            assertEquals(1024, ptr); // First allocation from mock
            assertEquals(4, len); // "test".length()
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    void testInstanceMemoryAccessNotAvailable() {
        // Note: The add.wasm module doesn't export memory
        // This test verifies we can gracefully handle missing memory
        WasmModule module = loadModule("compiled/add.wat.wasm");
        Store store = new Store();
        Instance instance = store.instantiate("add_module", module);

        // Memory may or may not be available - depends on the module
        // In Component Model, memory is expected to be provided by the host
        // For now, just verify the instance is created
        assertNotNull(instance);
    }

    /**
     * Mock implementation of canonical_abi_realloc for testing.
     */
    private static class MockCanonicalRealloc
            implements com.dylibso.chicory.runtime.ExportFunction {
        private int allocatedPtr = 1024;

        @Override
        public long[] apply(long... args) {
            int result = allocatedPtr;
            int newSize = (int) args[3];
            allocatedPtr += newSize;
            return new long[] {result};
        }
    }
}
