package com.dylibso.chicory.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Canonical ABI")
class CanonicalAbiTest {
    private Memory memory;

    @BeforeEach
    void setUp() {
        memory = new ByteBufferMemory(new MemoryLimits(1, 10));
    }

    @Nested
    @DisplayName("Numeric Types")
    class NumericTypeTests {
        @Test
        @DisplayName("encodes and decodes i32 values")
        void encodesAndDecodesI32Values() {
            long[] encoded = CanonicalAbi.encode(42, PrimitiveType.I32, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.I32, memory);

            assertEquals(1, encoded.length);
            assertEquals(42, encoded[0]);
            assertEquals(42, decoded);
        }

        @Test
        @DisplayName("encodes and decodes i64 values")
        void encodesAndDecodesI64Values() {
            long value = 1234567890123L;
            long[] encoded = CanonicalAbi.encode(value, PrimitiveType.I64, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.I64, memory);

            assertEquals(1, encoded.length);
            assertEquals(value, encoded[0]);
            assertEquals(value, decoded);
        }

        @Test
        @DisplayName("encodes and decodes f32 values")
        void encodesAndDecodesF32Values() {
            float value = 3.14f;
            long[] encoded = CanonicalAbi.encode(value, PrimitiveType.F32, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.F32, memory);

            assertEquals(1, encoded.length);
            assertEquals(value, (float) decoded, 0.001f);
        }

        @Test
        @DisplayName("encodes and decodes f64 values")
        void encodesAndDecodesF64Values() {
            double value = 3.14159265359;
            long[] encoded = CanonicalAbi.encode(value, PrimitiveType.F64, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.F64, memory);

            assertEquals(1, encoded.length);
            assertEquals(value, (double) decoded, 0.0000001);
        }
    }

    @Nested
    @DisplayName("Scalar Values")
    class ScalarValueTests {
        @Test
        @DisplayName("encodes and decodes true booleans")
        void encodesAndDecodesTrueBooleans() {
            long[] encoded = CanonicalAbi.encode(true, PrimitiveType.BOOL, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.BOOL, memory);

            assertEquals(1, encoded.length);
            assertEquals(1, encoded[0]);
            assertTrue((Boolean) decoded);
        }

        @Test
        @DisplayName("encodes and decodes false booleans")
        void encodesAndDecodesFalseBooleans() {
            long[] encoded = CanonicalAbi.encode(false, PrimitiveType.BOOL, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.BOOL, memory);

            assertEquals(1, encoded.length);
            assertEquals(0, encoded[0]);
            assertFalse((Boolean) decoded);
        }

        @Test
        @DisplayName("encodes and decodes chars")
        void encodesAndDecodesChars() {
            long[] encoded = CanonicalAbi.encode('A', PrimitiveType.CHAR, memory);
            Object decoded = CanonicalAbi.decode(encoded, PrimitiveType.CHAR, memory);

            assertEquals(1, encoded.length);
            assertEquals('A', encoded[0]);
            assertEquals('A', decoded);
        }

        @Test
        @DisplayName("encodes null values as zero")
        void encodesNullValuesAsZero() {
            long[] encoded = CanonicalAbi.encode(null, PrimitiveType.I32, memory);
            assertEquals(1, encoded.length);
            assertEquals(0, encoded[0]);
        }
    }

    @Nested
    @DisplayName("Composite Types")
    class CompositeTypeTests {
        @Test
        @DisplayName("encodes list values as pointer-count pairs")
        void encodesListValuesAsPointerCountPairs() {
            long[] encoded =
                    CanonicalAbi.encode(
                            Arrays.asList(1, 2, 3), new ListType(PrimitiveType.I32), memory);
            assertEquals(2, encoded.length);
        }

        @Test
        @DisplayName("encodes record values as pointers")
        void encodesRecordValuesAsPointers() {
            Map<String, Object> record = new HashMap<>();
            record.put("x", 10);
            record.put("y", 20);

            RecordType recordType = new RecordType("point");
            recordType.addField("x", PrimitiveType.I32);
            recordType.addField("y", PrimitiveType.I32);

            long[] encoded = CanonicalAbi.encode(record, recordType, memory);
            assertEquals(1, encoded.length);
        }
    }
}
