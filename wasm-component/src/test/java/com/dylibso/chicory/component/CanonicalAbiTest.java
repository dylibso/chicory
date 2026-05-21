package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Canonical ABI encoding/decoding.
 */
public class CanonicalAbiTest {

    private Memory memory;

    @BeforeEach
    public void setUp() {
        // Create a memory with 1 initial page and max 10 pages
        MemoryLimits limits = new MemoryLimits(1, 10);
        memory = new ByteBufferMemory(limits);
    }

    @Test
    public void testEncodeDecodeI32() {
        long[] encoded = CanonicalAbi.encode(42, PrimitiveType.I32, memory);
        assertEquals(1, encoded.length);
        assertEquals(42, encoded[0]);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.I32, memory);
        assertEquals(42, decoded);
    }

    @Test
    public void testEncodeDecodeI64() {
        long value = 1234567890123L;
        long[] encoded = CanonicalAbi.encode(value, PrimitiveType.I64, memory);
        assertEquals(1, encoded.length);
        assertEquals(value, encoded[0]);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.I64, memory);
        assertEquals(value, decoded);
    }

    @Test
    public void testEncodeDecodeF32() {
        float value = 3.14f;
        long[] encoded = CanonicalAbi.encode(value, PrimitiveType.F32, memory);
        assertEquals(1, encoded.length);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.F32, memory);
        assertEquals(value, (float) decoded, 0.001f);
    }

    @Test
    public void testEncodeDecodeF64() {
        double value = 3.14159265359;
        long[] encoded = CanonicalAbi.encode(value, PrimitiveType.F64, memory);
        assertEquals(1, encoded.length);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.F64, memory);
        assertEquals(value, (double) decoded, 0.0000001);
    }

    @Test
    public void testEncodeDecodeBoolTrue() {
        long[] encoded = CanonicalAbi.encode(true, PrimitiveType.BOOL, memory);
        assertEquals(1, encoded.length);
        assertEquals(1, encoded[0]);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.BOOL, memory);
        assertTrue((Boolean) decoded);
    }

    @Test
    public void testEncodeDecodeBoolFalse() {
        long[] encoded = CanonicalAbi.encode(false, PrimitiveType.BOOL, memory);
        assertEquals(1, encoded.length);
        assertEquals(0, encoded[0]);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.BOOL, memory);
        assertFalse((Boolean) decoded);
    }

    @Test
    public void testEncodeDecodeChar() {
        char value = 'A';
        long[] encoded = CanonicalAbi.encode(value, PrimitiveType.CHAR, memory);
        assertEquals(1, encoded.length);
        assertEquals('A', encoded[0]);

        Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.CHAR, memory);
        assertEquals(value, decoded);
    }

    @Test
    public void testEncodeNullValue() {
        long[] encoded = CanonicalAbi.encode(null, PrimitiveType.I32, memory);
        assertEquals(1, encoded.length);
        assertEquals(0, encoded[0]);
    }
}
