package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Memory;
import java.nio.charset.StandardCharsets;

/**
 * Implements the Canonical ABI (Application Binary Interface) for Component Model.
 * Handles encoding/decoding of WIT types to/from Wasm memory.
 *
 * Canonical ABI spec:
 * https://github.com/WebAssembly/component-model/blob/main/design/mvp/CanonicalABI.md
 */
public class CanonicalAbi {

    private static final ThreadLocal<ExportFunction> REALLOC_CONTEXT = new ThreadLocal<>();

    /**
     * Set the realloc function for the current encoding context.
     * Must be called before encoding strings/lists/records that require memory allocation.
     */
    public static void withRealloc(ExportFunction realloc) {
        REALLOC_CONTEXT.set(realloc);
    }

    /**
     * Clear the realloc context after encoding.
     */
    public static void clearRealloc() {
        REALLOC_CONTEXT.remove();
    }

    /**
     * Encode a Java value to WIT representation (as long[] for function arguments).
     *
     * @param value Java value to encode
     * @param type WIT type specification
     * @param memory Guest's linear memory
     * @return Encoded representation as long array (for function call arguments)
     */
    public static long[] encode(Object value, WitType type, Memory memory) {
        if (type instanceof PrimitiveType) {
            return encodePrimitive(value, (PrimitiveType) type);
        } else if (type instanceof ListType) {
            return encodeList(value, (ListType) type, memory);
        } else if (type instanceof RecordType) {
            return encodeRecord(value, (RecordType) type, memory);
        }
        throw new IllegalArgumentException("Cannot encode type: " + type.displayName());
    }

    /**
     * Decode a WIT value (from long[] function arguments) to Java value.
     *
     * @param encoded Encoded WIT value (from function arguments/returns)
     * @param type WIT type specification
     * @param memory Guest's linear memory
     * @return Decoded Java value
     */
    public static Object decode(long[] encoded, WitType type, Memory memory) {
        if (type instanceof PrimitiveType) {
            return decodePrimitive(encoded[0], (PrimitiveType) type, memory);
        } else if (type instanceof ListType) {
            return decodeList(encoded, (ListType) type, memory);
        } else if (type instanceof RecordType) {
            return decodeRecord(encoded, (RecordType) type, memory);
        }
        throw new IllegalArgumentException("Cannot decode type: " + type.displayName());
    }

    // Primitive type encoding
    private static long[] encodePrimitive(Object value, PrimitiveType type) {
        if (value == null) {
            return new long[] {0};
        }

        switch (type) {
            case I32:
                return new long[] {((Number) value).intValue() & 0xFFFFFFFFL};
            case I64:
                return new long[] {((Number) value).longValue()};
            case F32:
                return new long[] {
                    Float.floatToIntBits(((Number) value).floatValue()) & 0xFFFFFFFFL
                };
            case F64:
                return new long[] {Double.doubleToLongBits(((Number) value).doubleValue())};
            case BOOL:
                return new long[] {((Boolean) value) ? 1 : 0};
            case CHAR:
                return new long[] {((Character) value) & 0xFFFFFFFFL};
            case STRING:
                return encodeString((String) value);
            default:
                throw new IllegalArgumentException("Unknown primitive: " + type);
        }
    }

    // Primitive type decoding
    private static Object decodePrimitive(long value, PrimitiveType type, Memory memory) {
        switch (type) {
            case I32:
                return (int) value;
            case I64:
                return value;
            case F32:
                return Float.intBitsToFloat((int) value);
            case F64:
                return Double.longBitsToDouble(value);
            case BOOL:
                return value != 0;
            case CHAR:
                return (char) value;
            case STRING:
                return decodeString(value, memory);
            default:
                throw new IllegalArgumentException("Unknown primitive: " + type);
        }
    }

    // String encoding: allocate in guest memory and return (ptr, len)
    private static long[] encodeString(String value) {
        if (value == null) {
            // null string is (0, 0)
            return new long[] {0};
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ExportFunction realloc = REALLOC_CONTEXT.get();

        if (realloc != null) {
            // Use cabi_realloc to allocate in guest memory
            long[] result = realloc.apply(0, 0, 1, bytes.length);
            int ptr = (int) result[0];

            // In real usage, would write bytes to guest memory here
            // For now, we assume the guest will handle this via import functions
            // (In a full implementation, we'd need Memory reference to write bytes)

            long len = bytes.length & 0xFFFFFFFFL;
            return new long[] {(((long) ptr) << 32) | len};
        } else {
            // Fallback: return placeholder (for testing without real guest)
            long len = bytes.length & 0xFFFFFFFFL;
            return new long[] {len}; // Just return length for MVP testing
        }
    }

    // String decoding: read from guest memory using (ptr, len) pair
    private static String decodeString(long encoded, Memory memory) {
        if (encoded == 0) {
            return "";
        }

        int ptr = (int) (encoded >>> 32);
        int len = (int) encoded;

        if (len <= 0) {
            return "";
        }

        return memory.readString(ptr, len, StandardCharsets.UTF_8);
    }

    private static long[] encodeList(Object value, ListType type, Memory memory) {
        if (value == null) {
            // null list is (0, 0)
            return new long[] {0, 0};
        }

        if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            ExportFunction realloc = REALLOC_CONTEXT.get();

            if (realloc != null) {
                // Use cabi_realloc to allocate in guest memory
                // For MVP: assume each element is 8 bytes (worst case i64)
                int elementSize = 8;
                int totalSize = list.size() * elementSize;

                long[] result = realloc.apply(0, 0, 8, totalSize);
                long ptr = result[0];
                long count = list.size() & 0xFFFFFFFFL;

                return new long[] {ptr, count};
            } else {
                // Fallback: return placeholder (for testing without real guest)
                long count = list.size() & 0xFFFFFFFFL;
                return new long[] {0, count};
            }
        }

        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            ExportFunction realloc = REALLOC_CONTEXT.get();

            if (realloc != null) {
                int elementSize = 8;
                int totalSize = arr.length * elementSize;

                long[] result = realloc.apply(0, 0, 8, totalSize);
                long ptr = result[0];
                long count = arr.length & 0xFFFFFFFFL;

                return new long[] {ptr, count};
            } else {
                long count = arr.length & 0xFFFFFFFFL;
                return new long[] {0, count};
            }
        }

        throw new IllegalArgumentException("Cannot encode list from: " + value.getClass());
    }

    private static Object decodeList(long[] encoded, ListType type, Memory memory) {
        if (encoded.length < 2 || (encoded[0] == 0 && encoded[1] == 0)) {
            return new java.util.ArrayList<>();
        }

        int ptr = (int) encoded[0];
        int count = (int) encoded[1];

        if (count <= 0) {
            return new java.util.ArrayList<>();
        }

        // For MVP: return list of raw pointers
        // In real implementation, would decode each element from memory
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(null); // TODO: decode elements from memory
        }
        return result;
    }

    private static long[] encodeRecord(Object value, RecordType type, Memory memory) {
        if (value == null) {
            return new long[] {0};
        }

        ExportFunction realloc = REALLOC_CONTEXT.get();

        if (realloc != null) {
            // For MVP: allocate fixed size for record (assume max 256 bytes per record)
            // In real implementation, would calculate based on field types
            int recordSize = 256;

            long[] result = realloc.apply(0, 0, 8, recordSize);
            long ptr = result[0];

            return new long[] {ptr};
        } else {
            // Fallback: return placeholder
            return new long[] {0};
        }
    }

    private static Object decodeRecord(long[] encoded, RecordType type, Memory memory) {
        if (encoded.length == 0 || encoded[0] == 0) {
            return new java.util.HashMap<>();
        }

        int ptr = (int) encoded[0];
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // For MVP: return empty map
        // In real implementation, would read fields from memory
        for (RecordType.Field field : type.fields()) {
            result.put(field.name, null); // TODO: decode field from memory
        }

        return result;
    }
}
