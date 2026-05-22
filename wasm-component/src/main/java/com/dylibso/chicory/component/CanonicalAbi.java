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
    private static final ThreadLocal<Memory> MEMORY_CONTEXT = new ThreadLocal<>();

    /**
     * Set the realloc function and memory for the current encoding context.
     * Must be called before encoding strings/lists/records that require memory allocation.
     */
    public static void withContext(ExportFunction realloc, Memory memory) {
        REALLOC_CONTEXT.set(realloc);
        MEMORY_CONTEXT.set(memory);
    }

    /**
     * Set the realloc function for the current encoding context.
     * Must be called before encoding strings/lists/records that require memory allocation.
     */
    public static void withRealloc(ExportFunction realloc) {
        REALLOC_CONTEXT.set(realloc);
    }

    /**
     * Clear the context after encoding.
     */
    public static void clearContext() {
        REALLOC_CONTEXT.remove();
        MEMORY_CONTEXT.remove();
    }

    /**
     * Clear the realloc context after encoding. (Deprecated: use clearContext)
     */
    @Deprecated
    public static void clearRealloc() {
        clearContext();
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
            return encodePrimitive(value, (PrimitiveType) type, memory);
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
            if (encoded.length == 0) {
                return null;
            }
            return decodePrimitive(encoded, (PrimitiveType) type, memory);
        } else if (type instanceof ListType) {
            return decodeList(encoded, (ListType) type, memory);
        } else if (type instanceof RecordType) {
            return decodeRecord(encoded, (RecordType) type, memory);
        }
        throw new IllegalArgumentException("Cannot decode type: " + type.displayName());
    }

    private static long[] encodePrimitive(Object value, PrimitiveType type, Memory memory) {
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
                return encodeString((String) value, memory);
            default:
                throw new IllegalArgumentException("Unknown primitive: " + type);
        }
    }

    private static Object decodePrimitive(long[] encoded, PrimitiveType type, Memory memory) {
        if (encoded.length == 0) {
            return null;
        }

        long value = encoded[0];

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
                return decodeString(encoded, memory);
            default:
                throw new IllegalArgumentException("Unknown primitive: " + type);
        }
    }

    private static long[] encodeString(String value, Memory memory) {
        if (value == null || value.isEmpty()) {
            return new long[] {0, 0};
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ExportFunction realloc = REALLOC_CONTEXT.get();

        if (realloc != null) {
            long[] result = realloc.apply(0, 0, 1, bytes.length);
            long ptr = result[0];

            if (memory != null) {
                for (int i = 0; i < bytes.length; i++) {
                    memory.writeByte((int) ptr + i, bytes[i]);
                }
            }

            long len = bytes.length & 0xFFFFFFFFL;
            return new long[] {ptr, len};
        } else {
            long len = bytes.length & 0xFFFFFFFFL;
            return new long[] {0, len};
        }
    }

    private static String decodeString(long[] encoded, Memory memory) {
        if (encoded == null || encoded.length == 0 || memory == null) {
            return "";
        }

        // wit-bindgen returns a pointer to a (ptr, len) pair structure
        // encoded[0] = pointer to the structure
        int structPtr = (int) encoded[0];

        // Read the (ptr, len) pair from the structure
        int stringPtr = memory.readInt(structPtr);
        int stringLen = memory.readInt(structPtr + 4);

        if (stringLen <= 0) {
            return "";
        }

        return memory.readString(stringPtr, stringLen, StandardCharsets.UTF_8);
    }

    private static long[] encodeList(Object value, ListType type, Memory memory) {
        if (value == null) {
            return new long[] {0, 0};
        }

        if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            ExportFunction realloc = REALLOC_CONTEXT.get();

            if (realloc != null) {
                int elementSize = 8;
                int totalSize = list.size() * elementSize;

                long[] result = realloc.apply(0, 0, 8, totalSize);
                long ptr = result[0];
                long count = list.size() & 0xFFFFFFFFL;

                return new long[] {ptr, count};
            } else {
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

        java.util.List<Object> result = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(null);
        }
        return result;
    }

    private static long[] encodeRecord(Object value, RecordType type, Memory memory) {
        if (value == null) {
            return new long[] {0};
        }

        RecordLayout layout = new RecordLayout(type);
        ExportFunction realloc = REALLOC_CONTEXT.get();

        if (realloc != null) {
            // Allocate memory for the record
            long[] result = realloc.apply(0, 0, 8, layout.getRecordSize());
            long ptr = result[0];

            // Marshal fields into memory
            if (value instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                for (RecordLayout.FieldLayout field : layout.getFieldLayouts()) {
                    Object fieldValue = map.get(field.name);
                    long[] encoded_field = encode(fieldValue, field.type, memory);

                    // Write encoded field to record memory at offset
                    writeFieldToMemory(memory, ptr + field.offset, field.type, encoded_field);
                }
            } else {
                // Handle other object types (reflection-based marshalling)
                // For now, assume Map<String, Object>
                throw new IllegalArgumentException(
                        "Record values must be Map<String, Object>: " + value.getClass());
            }

            return new long[] {ptr};
        } else {
            return new long[] {0};
        }
    }

    /**
     * Write an encoded field value to record memory at the given offset.
     */
    private static void writeFieldToMemory(
            Memory memory, long fieldAddr, WitType fieldType, long[] encoded) {
        if (fieldType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) fieldType;
            switch (prim) {
                case I32:
                case F32:
                    memory.writeI32((int) fieldAddr, (int) encoded[0]);
                    break;
                case I64:
                case F64:
                    memory.writeLong((int) fieldAddr, encoded[0]);
                    break;
                case STRING:
                    // String is (ptr, len) pair
                    memory.writeI32((int) fieldAddr, (int) encoded[0]); // ptr
                    memory.writeI32((int) fieldAddr + 4, (int) encoded[1]); // len
                    break;
                case BOOL:
                case CHAR:
                    memory.writeByte((int) fieldAddr, (byte) encoded[0]);
                    break;
                default:
                    break;
            }
        }
    }

    private static Object decodeRecord(long[] encoded, RecordType type, Memory memory) {
        if (encoded.length == 0 || encoded[0] == 0) {
            return new java.util.HashMap<>();
        }

        RecordLayout layout = new RecordLayout(type);
        long recordPtr = encoded[0];
        java.util.Map<String, Object> record = new java.util.HashMap<>();

        // Unmarshal each field from memory
        for (RecordLayout.FieldLayout field : layout.getFieldLayouts()) {
            long fieldAddr = recordPtr + field.offset;
            long[] fieldEncoded = readFieldFromMemory(memory, fieldAddr, field.type);
            Object fieldValue = decode(fieldEncoded, field.type, memory);
            record.put(field.name, fieldValue);
        }

        return record;
    }

    /**
     * Read an encoded field value from record memory at the given offset.
     */
    private static long[] readFieldFromMemory(Memory memory, long fieldAddr, WitType fieldType) {
        if (fieldType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) fieldType;
            switch (prim) {
                case I32:
                case F32:
                    return new long[] {memory.readInt((int) fieldAddr)};
                case I64:
                case F64:
                    return new long[] {memory.readLong((int) fieldAddr)};
                case STRING:
                    // String is (ptr, len) pair
                    long ptr = memory.readInt((int) fieldAddr);
                    long len = memory.readInt((int) fieldAddr + 4);
                    return new long[] {ptr, len};
                case BOOL:
                case CHAR:
                    return new long[] {memory.read((int) fieldAddr)};
                default:
                    return new long[] {0};
            }
        }
        return new long[] {0};
    }
}
