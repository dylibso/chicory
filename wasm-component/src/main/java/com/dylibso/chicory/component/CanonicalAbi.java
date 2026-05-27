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

        java.util.List<?> list = null;

        if (value instanceof java.util.List) {
            list = (java.util.List<?>) value;
        } else if (value instanceof Object[]) {
            list = java.util.Arrays.asList((Object[]) value);
        } else if (value instanceof ListValue) {
            list = ((ListValue) value).elements();
        } else {
            throw new IllegalArgumentException("Cannot encode list from: " + value.getClass());
        }

        if (list.isEmpty()) {
            return new long[] {0, 0};
        }

        WitType elementType = type.elementType();
        ExportFunction realloc = REALLOC_CONTEXT.get();

        if (realloc == null) {
            // Without realloc, can't allocate memory
            return new long[] {0, list.size() & 0xFFFFFFFFL};
        }

        // Calculate element size based on type
        int elementSize = calculateElementSize(elementType);
        int totalSize = list.size() * elementSize;

        // Allocate memory for all elements
        long[] allocResult = realloc.apply(0, 0, elementSize, totalSize);
        long ptr = allocResult[0];

        // Encode each element into memory
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            long offset = ptr + (i * elementSize);
            encodeElementToMemory(element, elementType, offset, elementSize, memory);
        }

        return new long[] {ptr, list.size() & 0xFFFFFFFFL};
    }

    private static Object decodeList(long[] encoded, ListType type, Memory memory) {
        if (encoded.length < 2 || (encoded[0] == 0 && encoded[1] == 0)) {
            return new ListValue(new java.util.ArrayList<>());
        }

        int ptr = (int) encoded[0];
        int count = (int) encoded[1];

        if (count <= 0) {
            return new ListValue(new java.util.ArrayList<>());
        }

        WitType elementType = type.elementType();
        int elementSize = calculateElementSize(elementType);
        java.util.List<Object> result = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++) {
            long offset = ptr + (i * elementSize);
            Object element = decodeElementFromMemory(elementType, offset, elementSize, memory);
            result.add(element);
        }

        return new ListValue(result);
    }

    /**
     * Calculate the byte size of an element based on its type.
     */
    private static int calculateElementSize(WitType elementType) {
        if (elementType instanceof PrimitiveType) {
            switch ((PrimitiveType) elementType) {
                case I32:
                case F32:
                    return 4;
                case I64:
                case F64:
                    return 8;
                case BOOL:
                case CHAR:
                    return 1;
                case STRING:
                    // Strings are (ptr, len) pairs = 8 bytes each
                    return 8;
                default:
                    return 8;
            }
        } else if (elementType instanceof RecordType) {
            // Records have fixed size based on their layout
            RecordLayout layout = new RecordLayout((RecordType) elementType);
            return layout.getRecordSize();
        } else if (elementType instanceof VariantType) {
            // Variants: discriminant (4) + max payload size
            // For simplicity, use 8 bytes (discriminant + one i64)
            return 8;
        } else if (elementType instanceof ListType) {
            // Lists are (ptr, count) = 8 bytes
            return 8;
        }
        return 8; // Default
    }

    /**
     * Encode a single list element into memory at a given offset.
     */
    private static void encodeElementToMemory(
            Object element, WitType elementType, long offset, int elementSize, Memory memory) {
        if (memory == null) {
            // No memory to write to (e.g., in test scenarios with realloc only)
            return;
        }

        if (elementType instanceof PrimitiveType) {
            encodePrimitiveToMemory(element, (PrimitiveType) elementType, offset, memory);
        } else if (elementType instanceof RecordType) {
            encodeRecordToMemory(element, (RecordType) elementType, offset, memory);
        } else if (elementType instanceof VariantType) {
            encodeVariantToMemory(element, (VariantType) elementType, offset, memory);
        } else if (elementType instanceof ListType) {
            encodeListToMemory(element, (ListType) elementType, offset, memory);
        }
    }

    /**
     * Decode a single list element from memory at a given offset.
     */
    private static Object decodeElementFromMemory(
            WitType elementType, long offset, int elementSize, Memory memory) {
        if (elementType instanceof PrimitiveType) {
            return decodePrimitiveFromMemory((PrimitiveType) elementType, (int) offset, memory);
        } else if (elementType instanceof RecordType) {
            return decodeRecordFromMemory(offset, (RecordType) elementType, memory);
        } else if (elementType instanceof VariantType) {
            return decodeVariantFromMemory(offset, (VariantType) elementType, memory);
        } else if (elementType instanceof ListType) {
            return decodeListFromMemory((int) offset, (ListType) elementType, memory);
        }
        return null;
    }

    private static void encodePrimitiveToMemory(
            Object value, PrimitiveType type, long offset, Memory memory) {
        switch (type) {
            case I32:
                memory.writeI32((int) offset, ((Number) value).intValue());
                break;
            case I64:
                memory.writeLong((int) offset, ((Number) value).longValue());
                break;
            case F32:
                memory.writeF32((int) offset, ((Number) value).floatValue());
                break;
            case F64:
                memory.writeF64((int) offset, ((Number) value).doubleValue());
                break;
            case BOOL:
                memory.writeByte((int) offset, (byte) (((Boolean) value) ? 1 : 0));
                break;
            case CHAR:
                memory.writeByte((int) offset, (byte) ((Character) value).charValue());
                break;
            case STRING:
                if (value instanceof String) {
                    byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    ExportFunction realloc = REALLOC_CONTEXT.get();
                    if (realloc != null) {
                        long[] result = realloc.apply(0, 0, 1, bytes.length);
                        long ptr = result[0];
                        memory.write((int) ptr, bytes);
                        memory.writeI32((int) offset, (int) ptr);
                        memory.writeI32((int) offset + 4, bytes.length);
                    }
                }
                break;
        }
    }

    private static Object decodePrimitiveFromMemory(PrimitiveType type, int offset, Memory memory) {
        switch (type) {
            case I32:
                return memory.readInt(offset);
            case I64:
                return memory.readLong(offset);
            case F32:
                return memory.readFloat(offset);
            case F64:
                return memory.readDouble(offset);
            case BOOL:
                return memory.read(offset) != 0;
            case CHAR:
                return (char) memory.read(offset);
            case STRING:
                int ptr = memory.readInt(offset);
                int len = memory.readInt(offset + 4);
                System.out.println(
                        "[DEBUG string] Reading string at offset 0x"
                                + Integer.toHexString(offset)
                                + ": ptr=0x"
                                + Integer.toHexString(ptr)
                                + ", len="
                                + len);
                String str = memory.readString(ptr, len);
                System.out.println("[DEBUG string] Result: '" + str + "'");
                return str;
            default:
                return null;
        }
    }

    private static void encodeRecordToMemory(
            Object element, RecordType recordType, long offset, Memory memory) {
        // For records in lists, we need to encode the record data directly at the offset
        // This is similar to record layout encoding
        java.util.Map<?, ?> map = null;

        if (element instanceof java.util.Map) {
            map = (java.util.Map<?, ?>) element;
        } else {
            // Convert POJO to Map using reflection
            map = pojoToMap(element);
        }

        if (map != null) {
            RecordLayout layout = new RecordLayout(recordType);
            for (RecordLayout.FieldLayout field : layout.getFieldLayouts()) {
                Object fieldValue = map.get(field.name);
                long fieldOffset = offset + field.offset;
                int fieldSize = RecordLayout.getTypeSize(field.type);
                encodeElementToMemory(fieldValue, field.type, fieldOffset, fieldSize, memory);
            }
        }
    }

    /**
     * Convert a POJO to a Map by extracting fields using reflection.
     */
    private static java.util.Map<String, Object> pojoToMap(Object pojo) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();

        if (pojo == null) {
            return map;
        }

        try {
            // Get all fields including inherited ones
            java.lang.reflect.Field[] fields = pojo.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue = field.get(pojo);
                map.put(fieldName, fieldValue);
            }
        } catch (IllegalAccessException e) {
            // Ignore field access errors
        }

        return map;
    }

    private static void encodeVariantToMemory(
            Object element, VariantType variantType, long offset, Memory memory) {
        // For variants in lists, encode discriminant + data
        VariantValue variant = null;

        if (element instanceof VariantValue) {
            variant = (VariantValue) element;
        } else if (element != null) {
            // Try to convert POJO to VariantValue
            variant = pojoToVariant(element, variantType);
        }

        if (variant != null) {
            java.util.List<VariantType.Case> cases = variantType.cases();

            // Find discriminant for this case
            int discriminant = -1;
            VariantType.Case caseInfo = null;
            for (int i = 0; i < cases.size(); i++) {
                if (cases.get(i).name.equals(variant.caseName())) {
                    discriminant = i;
                    caseInfo = cases.get(i);
                    break;
                }
            }

            if (discriminant >= 0) {
                // Write discriminant
                memory.writeI32((int) offset, discriminant);

                // Write data if present
                if (caseInfo.type.isPresent() && variant.data() != null) {
                    int dataSize = RecordLayout.getTypeSize(caseInfo.type.get());
                    encodeElementToMemory(
                            variant.data(), caseInfo.type.get(), offset + 4, dataSize, memory);
                }
            }
        }
    }

    /**
     * Convert a variant POJO to VariantValue by finding which case it is.
     */
    private static VariantValue pojoToVariant(Object element, VariantType variantType) {
        if (element == null) {
            return null;
        }

        String className = element.getClass().getSimpleName();

        // Try to match variant case by class name
        for (VariantType.Case c : variantType.cases()) {
            if (c.name.equalsIgnoreCase(className)) {
                // Found matching case
                if (c.type.isPresent()) {
                    // Variant with data - extract data from POJO
                    return new VariantValue(c.name, element);
                } else {
                    // Empty variant
                    return new VariantValue(c.name, null);
                }
            }
        }

        // No matching case found
        return null;
    }

    private static void encodeListToMemory(
            Object element, ListType listType, long offset, Memory memory) {
        // For lists in lists, encode as (ptr, count)
        long[] encoded = encodeList(element, listType, memory);
        memory.writeI32((int) offset, (int) encoded[0]);
        memory.writeI32((int) offset + 4, (int) encoded[1]);
    }

    private static Object decodeListFromMemory(int offset, ListType listType, Memory memory) {
        int ptr = memory.readInt(offset);
        int count = memory.readInt(offset + 4);
        return decodeList(new long[] {ptr, count}, listType, memory);
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
            java.util.Map<String, Object> fieldMap;
            if (value instanceof java.util.Map) {
                fieldMap = (java.util.Map<String, Object>) value;
            } else {
                // Handle POJOs using reflection
                fieldMap = pojoToMap(value);
            }

            for (RecordLayout.FieldLayout field : layout.getFieldLayouts()) {
                Object fieldValue = fieldMap.get(field.name);
                long[] encoded_field = encode(fieldValue, field.type, memory);

                // Write encoded field to record memory at offset
                writeFieldToMemory(memory, ptr + field.offset, field.type, encoded_field);
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
