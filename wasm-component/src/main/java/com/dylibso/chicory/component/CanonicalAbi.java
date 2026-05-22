package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
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
     * Get the current realloc function from context.
     *
     * @return Current realloc function, or null if not set
     */
    public static ExportFunction getReallocFunction() {
        return REALLOC_CONTEXT.get();
    }

    /**
     * Decode a record directly from guest memory (sret convention).
     * Used when a function returns a record via structured return (sret).
     *
     * @param memoryPtr Pointer to record data in guest memory
     * @param recordType Record type specification
     * @param memory Guest's linear memory
     * @return Java Map representing the record with field names and values
     */
    public static Object decodeRecordFromMemory(
            long memoryPtr, RecordType recordType, Memory memory) {
        return decodeRecord(new long[] {memoryPtr}, recordType, memory);
    }

    /**
     * Decode a variant directly from guest memory.
     * Used when a function returns a variant (wit-bindgen returns pointer to variant data).
     *
     * @param memoryPtr Pointer to variant data in guest memory
     * @param variantType Variant type specification
     * @param memory Guest's linear memory
     * @return VariantValue representing the variant case and data
     */
    public static Object decodeVariantFromMemory(
            long memoryPtr, VariantType variantType, Memory memory) {
        // Read discriminant (u32) from memory
        int discriminant = memory.readInt((int) memoryPtr);

        // Remaining data follows the discriminant (at offset 4)
        java.util.List<VariantType.Case> cases = variantType.cases();

        if (discriminant < 0 || discriminant >= cases.size()) {
            throw new IllegalArgumentException("Invalid variant discriminant: " + discriminant);
        }

        VariantType.Case caseInfo = cases.get(discriminant);

        // Decode case data if present
        Object caseData = null;
        if (caseInfo.type.isPresent()) {
            // Read data from memory starting at offset 4 (after discriminant)
            long dataPtr = memoryPtr + 4;
            long[] caseEncoded = readVariantDataFromMemory(memory, dataPtr, caseInfo.type.get());
            caseData = decode(caseEncoded, caseInfo.type.get(), memory);
        }

        return new VariantValue(caseInfo.name, caseData);
    }

    /**
     * Read variant case data from memory based on type.
     */
    private static long[] readVariantDataFromMemory(
            Memory memory, long dataPtr, WitType fieldType) {
        if (fieldType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) fieldType;
            switch (prim) {
                case I32:
                case F32:
                    return new long[] {memory.readInt((int) dataPtr)};
                case I64:
                case F64:
                    return new long[] {memory.readLong((int) dataPtr)};
                case STRING:
                    // String is (ptr, len) pair
                    long ptr = memory.readInt((int) dataPtr);
                    long len = memory.readInt((int) dataPtr + 4);
                    return new long[] {ptr, len};
                case BOOL:
                case CHAR:
                    return new long[] {memory.read((int) dataPtr)};
                default:
                    return new long[] {0};
            }
        }
        return new long[] {0};
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
        } else if (type instanceof VariantType) {
            return encodeVariant(value, (VariantType) type, memory);
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
        } else if (type instanceof VariantType) {
            return decodeVariant(encoded, (VariantType) type, memory);
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

            // Special handling for STRING fields in records
            // In records, strings are stored as (ptr, len) pairs directly
            // (not as pointers to structures like in function returns)
            Object fieldValue;
            if (field.type instanceof PrimitiveType
                    && ((PrimitiveType) field.type) == PrimitiveType.STRING) {
                if (fieldEncoded.length >= 2) {
                    fieldValue = decodeStringFromPair(fieldEncoded[0], fieldEncoded[1], memory);
                } else {
                    fieldValue = "";
                }
            } else {
                fieldValue = decode(fieldEncoded, field.type, memory);
            }

            record.put(field.name, fieldValue);
        }

        return record;
    }

    /**
     * Decode a string directly from (ptr, len) pair (used in records).
     * This is different from decodeString which expects a pointer to the pair.
     */
    private static String decodeStringFromPair(long ptr, long len, Memory memory) {
        if (len <= 0 || memory == null) {
            return "";
        }
        return memory.readString((int) ptr, (int) len, StandardCharsets.UTF_8);
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
                    // String is (ptr, len) pair stored directly in record memory
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

    /**
     * Encode a VariantValue to WIT representation (discriminant + data).
     */
    private static long[] encodeVariant(Object value, VariantType type, Memory memory) {
        if (!(value instanceof VariantValue)) {
            throw new IllegalArgumentException(
                    "Expected VariantValue, got " + value.getClass().getSimpleName());
        }

        VariantValue variant = (VariantValue) value;
        java.util.List<Long> encoded = new java.util.ArrayList<>();

        // Find the case index (discriminant)
        int discriminant = -1;
        java.util.List<VariantType.Case> cases = type.cases();
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i).name.equals(variant.caseName())) {
                discriminant = i;
                break;
            }
        }

        if (discriminant == -1) {
            throw new IllegalArgumentException("Unknown variant case: " + variant.caseName());
        }

        // Add discriminant (u32)
        encoded.add((long) (discriminant & 0xFFFFFFFFL));

        // Add case data if present
        if (!variant.isEmpty()) {
            VariantType.Case caseInfo = cases.get(discriminant);
            if (caseInfo.type.isPresent()) {
                long[] caseEncoded = encode(variant.data(), caseInfo.type.get(), memory);
                for (long val : caseEncoded) {
                    encoded.add(val);
                }
            }
        }

        long[] result = new long[encoded.size()];
        for (int i = 0; i < encoded.size(); i++) {
            result[i] = encoded.get(i);
        }
        return result;
    }

    /**
     * Decode a variant from WIT representation (discriminant + data).
     */
    /**
     * Decode a variant from inline encoded values (not from memory).
     * Used when variant data is passed directly as return values.
     */
    public static Object decodeVariantInline(long[] encoded, VariantType type, Memory memory) {
        if (encoded.length == 0) {
            throw new IllegalArgumentException("Variant encoding must have at least discriminant");
        }

        // Read discriminant (first value is u32)
        int discriminant = (int) (encoded[0] & 0xFFFFFFFFL);
        java.util.List<VariantType.Case> cases = type.cases();

        if (discriminant < 0 || discriminant >= cases.size()) {
            throw new IllegalArgumentException("Invalid variant discriminant: " + discriminant);
        }

        VariantType.Case caseInfo = cases.get(discriminant);

        // Decode case data if present
        Object caseData = null;
        if (caseInfo.type.isPresent() && encoded.length > 1) {
            // Remaining values are the case data
            long[] caseEncoded = java.util.Arrays.copyOfRange(encoded, 1, encoded.length);
            caseData = decode(caseEncoded, caseInfo.type.get(), memory);
        }

        return new VariantValue(caseInfo.name, caseData);
    }

    private static Object decodeVariant(long[] encoded, VariantType type, Memory memory) {
        if (encoded.length == 0) {
            throw new IllegalArgumentException("Variant encoding must have at least discriminant");
        }

        // Read discriminant (first value is u32)
        int discriminant = (int) (encoded[0] & 0xFFFFFFFFL);
        java.util.List<VariantType.Case> cases = type.cases();

        if (discriminant < 0 || discriminant >= cases.size()) {
            throw new IllegalArgumentException("Invalid variant discriminant: " + discriminant);
        }

        VariantType.Case caseInfo = cases.get(discriminant);

        // Decode case data if present
        Object caseData = null;
        if (caseInfo.type.isPresent() && encoded.length > 1) {
            // Remaining values are the case data
            long[] caseEncoded = java.util.Arrays.copyOfRange(encoded, 1, encoded.length);
            caseData = decode(caseEncoded, caseInfo.type.get(), memory);
        }

        return new VariantValue(caseInfo.name, caseData);
    }
}
