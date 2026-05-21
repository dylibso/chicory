package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.types.PrimitiveType;
import org.junit.jupiter.api.Test;

/**
 * Tests for WIT parser.
 */
public class WitParserTest {

    @Test
    public void testParseSimpleFunction() {
        String wit = "add: function(a: i32, b: i32) -> i32";
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        assertNotNull(definition);
        assertEquals(1, definition.exports().size());

        ComponentDefinition.FunctionSignature func = definition.exports().get(0);
        assertEquals("add", func.name());
        assertEquals(2, func.parameters().size());
        assertEquals(1, func.returns().size());

        assertEquals("a", func.parameters().get(0).name);
        assertEquals(PrimitiveType.I32.displayName(), func.parameters().get(0).type.displayName());

        assertEquals("b", func.parameters().get(1).name);
        assertEquals(PrimitiveType.I32.displayName(), func.parameters().get(1).type.displayName());

        assertEquals(PrimitiveType.I32.displayName(), func.returns().get(0).displayName());
    }

    @Test
    public void testParseStringFunction() {
        String wit = "greet: function(name: string) -> string";
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        assertEquals(1, definition.exports().size());
        ComponentDefinition.FunctionSignature func = definition.exports().get(0);

        assertEquals("greet", func.name());
        assertEquals(1, func.parameters().size());
        assertEquals(
                PrimitiveType.STRING.displayName(), func.parameters().get(0).type.displayName());
        assertEquals(PrimitiveType.STRING.displayName(), func.returns().get(0).displayName());
    }

    @Test
    public void testParseNoParameters() {
        String wit = "get-count: function() -> i32";
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        ComponentDefinition.FunctionSignature func = definition.exports().get(0);
        assertEquals(0, func.parameters().size());
        assertEquals(1, func.returns().size());
    }

    @Test
    public void testParseNoReturn() {
        String wit = "print-message: function(msg: string)";
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        ComponentDefinition.FunctionSignature func = definition.exports().get(0);
        assertEquals(1, func.parameters().size());
        assertEquals(0, func.returns().size());
    }

    @Test
    public void testParseListType() {
        String wit = "process-items: function(items: list<i32>) -> i32";
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(wit);

        ComponentDefinition.FunctionSignature func = definition.exports().get(0);
        assertTrue(func.parameters().get(0).type.displayName().startsWith("list<"));
    }
}
