package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void invalidConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new Value(ValueType.I64, 42));
    }

    @Test
    public void equalsContract() {

        var i32FortyTwo = Value.i32(42);
        var i64FortyTwo = Value.i64(42L);
        var i32TwentyOne = Value.i32(21);
        var f32TwentyOne = Value.f32(Float.floatToIntBits(21.0f));

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

    @Test
    public void shouldConvertToArrays() {
        long x = 506097522914230528L;
        var result = Value.vecTo8(new Value[] {Value.v128(x)});

        assertEquals(8, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(3, result[3]);
        assertEquals(4, result[4]);
        assertEquals(5, result[5]);
        assertEquals(6, result[6]);
        assertEquals(7, result[7]);
    }

    @Test
    public void shouldConvertToArraysHL() {
        long xLow = 506097522914230528L;
        long xHigh = 1084818905618843912L;
        var result = Value.vecTo8(new Value[] {Value.v128(xLow), Value.v128(xHigh)});

        assertEquals(16, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(3, result[3]);
        assertEquals(4, result[4]);
        assertEquals(5, result[5]);
        assertEquals(6, result[6]);
        assertEquals(7, result[7]);
        assertEquals(8, result[8]);
        assertEquals(9, result[9]);
        assertEquals(10, result[10]);
        assertEquals(11, result[11]);
        assertEquals(12, result[12]);
        assertEquals(13, result[13]);
        assertEquals(14, result[14]);
        assertEquals(15, result[15]);
    }
}
