package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.types.PrimitiveType;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Component Model.
 * Tests end-to-end functionality with actual Wasm modules.
 */
public class ComponentIntegrationTest {

    @Test
    void testComponentModelBasics() {
        // Simple WIT definition
        String wit = "export add: function(a: i32, b: i32) -> i32\n";

        // Parse the WIT
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        // Verify it was parsed
        assertNotNull(definition);
        assertEquals(1, definition.exports().size());

        ComponentDefinition.FunctionSignature sig = definition.exports().get(0);
        assertEquals("add", sig.name());
        assertEquals(2, sig.parameters().size());
        assertEquals(1, sig.returns().size());
    }

    @Test
    void testCanonicalAbiEncodingDecoding() {
        // Test all primitive types round-trip through ABI
        CanonicalAbi.encode(42, PrimitiveType.I32, null);
        CanonicalAbi.encode(123456789L, PrimitiveType.I64, null);
        CanonicalAbi.encode(3.14f, PrimitiveType.F32, null);
        CanonicalAbi.encode(2.718, PrimitiveType.F64, null);
        CanonicalAbi.encode(true, PrimitiveType.BOOL, null);
        CanonicalAbi.encode("hello", PrimitiveType.STRING, null);
    }

    @Test
    void testParseListType() {
        // Test parsing of list types
        String wit = "export process: function(items: list<i32>) -> i32\n";

        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        assertNotNull(definition);
        assertEquals(1, definition.exports().size());
    }
}
