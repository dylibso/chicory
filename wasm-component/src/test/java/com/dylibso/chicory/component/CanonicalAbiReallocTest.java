package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.runtime.ExportFunction;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
@DisplayName("Canonical ABI Realloc")
class CanonicalAbiReallocTest {

    @AfterEach
    void clearRealloc() {
        CanonicalAbi.clearRealloc();
    }

    @Nested
    @DisplayName("String Encoding")
    class StringEncodingTests {
        @Test
        @DisplayName("encode uses realloc when available")
        void encodeUsesReallocWhenAvailable() {
            CanonicalAbi.withRealloc(new MockRealloc().getExportFunction());

            long[] encoded = CanonicalAbi.encode("hello", PrimitiveType.STRING, null);

            assertEquals(2, encoded.length);
            assertEquals(1024, encoded[0]);
            assertEquals(5, encoded[1]);
        }

        @Test
        @DisplayName("encode falls back to a null pointer without realloc")
        void encodeFallsBackWithoutRealloc() {
            long[] encoded = CanonicalAbi.encode("test", PrimitiveType.STRING, null);

            assertEquals(0, encoded[0]);
            assertEquals(4, encoded[1]);
        }
    }

    @Nested
    @DisplayName("List Encoding")
    class ListEncodingTests {
        @Test
        @DisplayName("encode uses realloc for lists")
        void encodeUsesReallocForLists() {
            CanonicalAbi.withRealloc(new MockRealloc().getExportFunction());

            long[] encoded =
                    CanonicalAbi.encode(
                            Arrays.asList(1, 2, 3), new ListType(PrimitiveType.I32), null);

            assertEquals(2, encoded.length);
            assertEquals(1024, encoded[0]);
            assertEquals(3, encoded[1]);
        }
    }

    @Nested
    @DisplayName("Allocation Lifecycle")
    class AllocationLifecycleTests {
        @Test
        @DisplayName("subsequent allocations advance the pointer")
        void subsequentAllocationsAdvanceThePointer() {
            CanonicalAbi.withRealloc(new MockRealloc().getExportFunction());

            long[] first = CanonicalAbi.encode("world", PrimitiveType.STRING, null);
            long[] second = CanonicalAbi.encode("foo", PrimitiveType.STRING, null);

            assertEquals(1024, first[0]);
            assertEquals(1029, second[0]);
        }
    }

    private static final class MockRealloc {
        private int nextAddr = 1024;

        private ExportFunction getExportFunction() {
            return args -> {
                int size = (int) args[3];
                int result = nextAddr;
                nextAddr += size;
                return new long[] {result};
            };
        }
    }
}
