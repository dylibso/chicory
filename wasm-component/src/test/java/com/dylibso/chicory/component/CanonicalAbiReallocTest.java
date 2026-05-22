package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.runtime.ExportFunction;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class CanonicalAbiReallocTest {

    @Test
    public void testEncodeStringWithRealloc() {
        // Mock realloc that returns predefined addresses
        MockRealloc realloc = new MockRealloc();
        CanonicalAbi.withRealloc(realloc.getExportFunction());

        try {
            long[] encoded = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);

            // Should return [ptr, len]
            assertEquals(2, encoded.length);
            assertEquals(1024, encoded[0]); // First allocation
            assertEquals(5, encoded[1]); // "hello".length()
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    public void testEncodeListWithRealloc() {
        MockRealloc realloc = new MockRealloc();
        CanonicalAbi.withRealloc(realloc.getExportFunction());

        try {
            java.util.List<Integer> list = java.util.Arrays.asList(1, 2, 3);
            long[] encoded =
                    CanonicalAbi.encode(
                            list,
                            new com.dylibso.chicory.component.types.ListType(PrimitiveType.I32),
                            null);

            // Should return [ptr, len]
            assertEquals(2, encoded.length);
            assertEquals(1024, encoded[0]); // allocated ptr
            assertEquals(3, encoded[1]); // list length
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    public void testEncodeMultipleWithRealloc() {
        MockRealloc realloc = new MockRealloc();
        CanonicalAbi.withRealloc(realloc.getExportFunction());

        try {
            // First allocation
            long[] enc1 = CanonicalAbi.encode("world", PrimitiveType.STRING, null);
            assertEquals(1024, enc1[0]);

            // Second allocation should get next address (1024 + 5 = 1029)
            long[] enc2 = CanonicalAbi.encode("foo", PrimitiveType.STRING, null);
            assertEquals(1029, enc2[0]);
        } finally {
            CanonicalAbi.clearRealloc();
        }
    }

    @Test
    public void testEncodeStringWithoutRealloc() {
        long[] encoded = CanonicalAbi.encode("test", PrimitiveType.STRING, null);

        // Without realloc, should return [0, len]
        assertEquals(0, encoded[0]); // null ptr
        assertEquals(4, encoded[1]); // "test".length()
    }

    /**
     * Mock realloc that simulates guest memory allocation.
     * Always returns incrementing addresses starting at 1024.
     */
    private static class MockRealloc {
        private int nextAddr = 1024;

        public ExportFunction getExportFunction() {
            return new ExportFunction() {
                @Override
                public long[] apply(long... args) {
                    int size = (int) args[3]; // 4th arg is size
                    int result = nextAddr;
                    nextAddr += size;
                    return new long[] {result};
                }
            };
        }
    }
}
