package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.*;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class ValueTest {
    @Test
    public void shouldEncodeValuesFromLong() {
        var i32 = Value.i32(123L);
        assertEquals("123@i32", i32.toString());
        var i64 = Value.i64(123L);
        assertEquals("123@i64", i64.toString());
        var f32 = Value.f32(123L);
        assertEquals("1.72E-43@f32", f32.toString());
        var f64 = Value.f64(123L);
        assertEquals("6.1E-322@f64", f64.toString());
    }

    @Test
    public void shouldConvertFloats() {
        var f32Ref = 0.12345678f;
        var f32 = Value.f32(1039980265L);
        assertEquals(f32Ref, f32.asFloat(), 0.0);
        assertArrayEquals(f32.getData(), Value.fromFloat(f32Ref).getData());
        var f64Ref = 0.123456789012345d;
        var f64 = Value.f64(4593560419847042606L);
        assertEquals(f64Ref, f64.asDouble(), 0.0);
        assertArrayEquals(f64.getData(), Value.fromDouble(f64Ref).getData());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Value.class)
                .withPrefabValues(Value.class, Value.i32(42), Value.i32(21))
                .verify();
    }
}
