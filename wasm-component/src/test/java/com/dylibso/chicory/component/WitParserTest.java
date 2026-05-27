package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.types.PrimitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WIT Parser")
class WitParserTest {

    @Nested
    @DisplayName("Function Signatures")
    class FunctionSignatureTests {
        @Test
        @DisplayName("parses simple functions")
        void parsesSimpleFunctions() {
            ComponentDefinition definition =
                    new WitParser().parse("add: function(a: i32, b: i32) -> i32");
            ComponentDefinition.FunctionSignature function = definition.exports().get(0);

            assertNotNull(definition);
            assertEquals(1, definition.exports().size());
            assertEquals("add", function.name());
            assertEquals(2, function.parameters().size());
            assertEquals(1, function.returns().size());
            assertEquals("a", function.parameters().get(0).name);
            assertEquals(
                    PrimitiveType.I32.displayName(),
                    function.parameters().get(0).type.displayName());
            assertEquals("b", function.parameters().get(1).name);
            assertEquals(
                    PrimitiveType.I32.displayName(),
                    function.parameters().get(1).type.displayName());
            assertEquals(PrimitiveType.I32.displayName(), function.returns().get(0).displayName());
        }

        @Test
        @DisplayName("parses string functions")
        void parsesStringFunctions() {
            ComponentDefinition.FunctionSignature function =
                    new WitParser()
                            .parse("greet: function(name: string) -> string")
                            .exports()
                            .get(0);

            assertEquals("greet", function.name());
            assertEquals(1, function.parameters().size());
            assertEquals(
                    PrimitiveType.STRING.displayName(),
                    function.parameters().get(0).type.displayName());
            assertEquals(
                    PrimitiveType.STRING.displayName(), function.returns().get(0).displayName());
        }

        @Test
        @DisplayName("parses functions without parameters")
        void parsesFunctionsWithoutParameters() {
            ComponentDefinition.FunctionSignature function =
                    new WitParser().parse("get-count: function() -> i32").exports().get(0);

            assertEquals(0, function.parameters().size());
            assertEquals(1, function.returns().size());
        }

        @Test
        @DisplayName("parses functions without returns")
        void parsesFunctionsWithoutReturns() {
            ComponentDefinition.FunctionSignature function =
                    new WitParser().parse("print-message: function(msg: string)").exports().get(0);

            assertEquals(1, function.parameters().size());
            assertEquals(0, function.returns().size());
        }

        @Test
        @DisplayName("parses list parameter types")
        void parsesListParameterTypes() {
            ComponentDefinition.FunctionSignature function =
                    new WitParser()
                            .parse("process-items: function(items: list<i32>) -> i32")
                            .exports()
                            .get(0);

            assertTrue(function.parameters().get(0).type.displayName().startsWith("list<"));
        }
    }

    @Nested
    @DisplayName("World Syntax")
    class WorldSyntaxTests {
        @Test
        @DisplayName("parses exported world functions")
        void parsesExportedWorldFunctions() {
            String wit =
                    "package example:example;\n\n"
                            + "world example {\n"
                            + "  export add: func(a: s32, b: s32) -> s32;\n"
                            + "  export multiply: func(a: s64, b: s64) -> s64;\n"
                            + "  export is-positive: func(x: s32) -> bool;\n"
                            + "}";

            ComponentDefinition definition = new WitParser().parse(wit);

            assertNotNull(definition);
            assertEquals("example:example", definition.packageName());
            assertEquals("example", definition.interfaceName());
            assertEquals(3, definition.exports().size());
            assertEquals("add", definition.exports().get(0).name());
            assertEquals("multiply", definition.exports().get(1).name());
            assertEquals("is-positive", definition.exports().get(2).name());
            assertEquals(2, definition.exports().get(0).parameters().size());
            assertEquals(1, definition.exports().get(2).returns().size());
        }

        @Test
        @DisplayName("parses worlds with imports")
        void parsesWorldsWithImports() {
            String wit =
                    "package example:api;\n\n"
                            + "world example {\n"
                            + "  export process: func(data: i32) -> i32;\n"
                            + "  import log: func(msg: string);\n"
                            + "}";

            ComponentDefinition definition = new WitParser().parse(wit);

            assertEquals(1, definition.exports().size());
            assertEquals(1, definition.imports().size());
            assertEquals("process", definition.exports().get(0).name());
            assertEquals("log", definition.imports().get(0).name());
        }
    }
}
