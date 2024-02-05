package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertArrayEquals(f32.data(), Value.fromFloat(f32Ref).data());
        var f64Ref = 0.123456789012345d;
        var f64 = Value.f64(4593560419847042606L);
        assertEquals(f64Ref, f64.asDouble(), 0.0);
        assertArrayEquals(f64.data(), Value.fromDouble(f64Ref).data());
    }

    @Test
    public void validConstruction() {

        new Value(ValueType.I32, 42);
        assertTrue(true);
    }

    @Test
    public void equalsContract() {

        var i32FortyTwo = Value.i32(42);
        var i64FortyTwo = Value.i64(42L);
        var i32TwentyOne = Value.i32(21);
        var f32TwentyOne = Value.f32(Float.floatToIntBits(21.f));

        assertEquals(i32FortyTwo, i32FortyTwo);
        assertEquals(i32FortyTwo, Value.i32(42));
        assertNotEquals(i32FortyTwo, i32TwentyOne);
        assertNotEquals(i32FortyTwo, null);
        assertNotEquals(i32TwentyOne, f32TwentyOne);
        assertNotEquals(i32FortyTwo, i64FortyTwo);
    }

    @Test
    public void hashCodeContract() {

        var i32FortyTwo = Value.i32(42);
        var i64FortyTwo = Value.i64(42L);

        assertEquals(i32FortyTwo.hashCode(), Value.i32(42).hashCode());
        assertNotEquals(i32FortyTwo.hashCode(), i64FortyTwo.hashCode());
    }

    @Test
    public void toStringContract() {

        var i32FortyTwo = Value.i32(42);
        assertNotNull(i32FortyTwo.toString());
    }
}
