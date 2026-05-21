package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.WitType;
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
        // Encode as (ptr << 32) | len in a single long
        // Note: This is a simplified encoding; actual allocation would require
        // calling cabi_realloc in the guest
        long ptr = 0; // TODO: allocate in guest memory via cabi_realloc
        long len = bytes.length & 0xFFFFFFFFL;
        return new long[] {(ptr << 32) | len};
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
        // TODO: Implement list encoding
        // Lists are encoded as (ptr, count)
        throw new UnsupportedOperationException("List encoding not yet implemented");
    }

    private static Object decodeList(long[] encoded, ListType type, Memory memory) {
        // TODO: Implement list decoding
        throw new UnsupportedOperationException("List decoding not yet implemented");
    }

    private static long[] encodeRecord(Object value, RecordType type, Memory memory) {
        // TODO: Implement record encoding
        // Records are laid out sequentially in memory
        throw new UnsupportedOperationException("Record encoding not yet implemented");
    }

    private static Object decodeRecord(long[] encoded, RecordType type, Memory memory) {
        // TODO: Implement record decoding
        throw new UnsupportedOperationException("Record decoding not yet implemented");
    }
}
