package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.types.Value;
import org.junit.Test;

import static org.junit.Assert.*;

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
}
