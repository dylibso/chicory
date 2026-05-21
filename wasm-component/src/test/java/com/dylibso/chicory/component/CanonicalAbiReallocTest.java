package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.runtime.ExportFunction;
import org.junit.jupiter.api.Test;

/**
 * Tests for Canonical ABI encoding with cabi_realloc integration.
 */
public class CanonicalAbiReallocTest {

    private static class MockRealloc implements ExportFunction {
        private int allocatedPtr = 1024; // Start allocations at offset 1024

        @Override
        public long[] apply(long... args) {
            int originalPtr = (int) args[0];
            int originalSize = (int) args[1];
            int alignment = (int) args[2];
            int newSize = (int) args[3];

            // Simple mock: return sequential addresses
            int result = allocatedPtr;
            allocatedPtr += newSize;

            return new long[] {result};
        }
    }

    @Test
    void testEncodeStringWithRealloc() {
        CanonicalAbi.withRealloc(new MockRealloc());
        try {
            long[] encoded = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);

            // Should return (ptr << 32) | len
            int ptr = (int) (encoded[0] >>> 32);
            int len = (int) encoded[0];

            assertEquals(1024, ptr); // First allocation
            assertEquals(5, len); // "hello".length()
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    void testEncodeListWithRealloc() {
        CanonicalAbi.withRealloc(new MockRealloc());
        try {
            java.util.List<Integer> list = java.util.Arrays.asList(1, 2, 3);
            long[] encoded = CanonicalAbi.encode(list, new ListType(PrimitiveType.I32), null);

            // Should return (ptr, count)
            int ptr = (int) encoded[0];
            int count = (int) encoded[1];

            assertEquals(1024, ptr); // First allocation
            assertEquals(3, count); // 3 elements
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    void testEncodeRecordWithRealloc() {
        CanonicalAbi.withRealloc(new MockRealloc());
        try {
            RecordType recordType = new RecordType("point");
            recordType.addField("x", PrimitiveType.I32);
            recordType.addField("y", PrimitiveType.I32);

            java.util.Map<String, Object> record = new java.util.HashMap<>();
            record.put("x", 10);
            record.put("y", 20);

            long[] encoded = CanonicalAbi.encode(record, recordType, null);

            // Should return (ptr)
            int ptr = (int) encoded[0];

            assertEquals(1024, ptr); // First allocation
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    void testEncodeMultipleWithRealloc() {
        CanonicalAbi.withRealloc(new MockRealloc());
        try {
            // First allocation
            long[] encoded1 = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);
            int ptr1 = (int) (encoded1[0] >>> 32);

            // Second allocation
            long[] encoded2 = CanonicalAbi.encode("world", PrimitiveType.STRING, null);
            int ptr2 = (int) (encoded2[0] >>> 32);

            // Should be different addresses
            assertEquals(1024, ptr1);
            assertEquals(1029, ptr2); // 1024 + "hello".length(5) = 1029
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    void testEncodeWithoutRealloc() {
        // Without realloc context, should still work (fallback mode)
        long[] encoded = CanonicalAbi.encode("test", PrimitiveType.STRING, null);

        // Should return fallback: just length
        assertEquals(4, encoded[0]); // "test".length()
    }
}
