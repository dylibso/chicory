package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.component.types.PrimitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Component Integration")
class ComponentIntegrationTest {

    @Nested
    @DisplayName("WIT Parsing")
    class WitParsingTests {
        @Test
        @DisplayName("parses basic component exports")
        void parsesBasicComponentExports() {
            ComponentDefinition definition =
                    new WitParser().parse("export add: function(a: i32, b: i32) -> i32\n");

            assertNotNull(definition);
            assertEquals(1, definition.exports().size());
            assertEquals("add", definition.exports().get(0).name());
            assertEquals(2, definition.exports().get(0).parameters().size());
            assertEquals(1, definition.exports().get(0).returns().size());
        }

        @Test
        @DisplayName("parses list parameters")
        void parsesListParameters() {
            ComponentDefinition definition =
                    new WitParser().parse("export process: function(items: list<i32>) -> i32\n");

            assertNotNull(definition);
            assertEquals(1, definition.exports().size());
            assertEquals(
                    "list<i32>",
                    definition.exports().get(0).parameters().get(0).type.displayName());
        }
    }

    @Nested
    @DisplayName("Canonical ABI")
    class CanonicalAbiTests {
        @Test
        @DisplayName("encodes primitive values without errors")
        void encodesPrimitiveValuesWithoutErrors() {
            assertEquals(42, CanonicalAbi.encode(42, PrimitiveType.I32, null)[0]);
            assertEquals(123456789L, CanonicalAbi.encode(123456789L, PrimitiveType.I64, null)[0]);
            assertEquals(1, CanonicalAbi.encode(true, PrimitiveType.BOOL, null)[0]);
            assertEquals(5, CanonicalAbi.encode("hello", PrimitiveType.STRING, null)[1]);
        }
    }
}
