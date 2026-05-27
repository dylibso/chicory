package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Typed wrapper around guest export function.
 * Provides type-safe interface for calling Wasm functions from Java,
 * handling Canonical ABI encoding/decoding automatically.
 */
public class TypedExportFunction {
    private final ExportFunction exportFunction;
    private final ComponentDefinition definition;
    private final ComponentDefinition.FunctionSignature signature;
    private final Memory memory;
    private final Instance instance;

    public TypedExportFunction(
            ExportFunction exportFunction,
            ComponentDefinition definition,
            ComponentDefinition.FunctionSignature signature,
            Memory memory,
            Instance instance) {
        this.exportFunction = exportFunction;
        this.definition = definition;
        this.signature = signature;
        this.memory = memory;
        this.instance = instance;
    }

    /**
     * Call the guest function with typed arguments.
     *
     * @param args Java objects matching the function's WIT signature
     * @return Decoded result from the guest function
     * @throws IllegalArgumentException if arguments don't match signature
     */
    public Object call(Object... args) {
        // Validate argument count
        List<ComponentDefinition.FunctionSignature.Parameter> params = signature.parameters();
        if (args.length != params.size()) {
            throw new IllegalArgumentException(
                    String.format("Expected %d arguments, got %d", params.size(), args.length));
        }

        // Set up realloc context for string/list encoding
        ExportFunction realloc = null;
        if (instance != null) {
            realloc = instance.export("cabi_realloc");
        }

        try {
            if (realloc != null) {
                CanonicalAbi.withContext(realloc, memory);
            }

            // Check if this function returns a record or variant
            List<WitType> returnTypes = signature.returns();
            boolean returnsRecord =
                    !returnTypes.isEmpty() && returnTypes.get(0) instanceof RecordType;
            boolean returnsVariant =
                    !returnTypes.isEmpty() && returnTypes.get(0) instanceof VariantType;
            boolean returnsList = !returnTypes.isEmpty() && returnTypes.get(0) instanceof ListType;
            RecordType returnRecordType = returnsRecord ? (RecordType) returnTypes.get(0) : null;
            VariantType returnVariantType =
                    returnsVariant ? (VariantType) returnTypes.get(0) : null;
            ListType returnListType = returnsList ? (ListType) returnTypes.get(0) : null;

            // Check if function takes lists as parameters
            boolean takesListParam = false;
            for (ComponentDefinition.FunctionSignature.Parameter param : params) {
                if (param.type instanceof ListType) {
                    takesListParam = true;
                    break;
                }
            }

            // Use fixed sret buffer for list-returning functions that take lists
            // Use a high memory address that should be safe (1 MB offset)
            // DISABLED: sret seems to cause unreachable instructions
            long sretPtr = 0;
            // if (returnsList && takesListParam) {
            //     sretPtr = 0x100000; // 1 MB - safe fixed buffer location
            //     System.out.println(
            //             "[DEBUG] Using fixed sret buffer at 0x" + Long.toHexString(sretPtr));
            // }

            // Encode arguments according to Canonical ABI
            List<Long> wasmArgsList = new ArrayList<>();

            // Add sret pointer as first parameter if needed
            if (sretPtr > 0) {
                wasmArgsList.add(sretPtr);
            }

            // Add regular parameters (no sret for records - WASM allocates and returns pointer)
            for (int i = 0; i < args.length; i++) {
                WitType paramType = params.get(i).type;
                long[] encoded;

                // Special handling for records: flatten them into individual parameters
                if (paramType instanceof RecordType) {
                    Map<String, Object> recordMap;
                    if (args[i] instanceof Map) {
                        recordMap = (Map<String, Object>) args[i];
                    } else {
                        // Convert POJO to Map
                        recordMap = pojoToMap(args[i]);
                    }
                    encoded =
                            RecordFlattener.flattenRecord(
                                    recordMap, (RecordType) paramType, memory);
                } else {
                    encoded = CanonicalAbi.encode(args[i], paramType, memory);
                }

                // Add all encoded values (strings use 2 values: ptr, len)
                for (long value : encoded) {
                    wasmArgsList.add(value);
                }
            }

            // Convert to array
            long[] wasmArgs = new long[wasmArgsList.size()];
            for (int i = 0; i < wasmArgsList.size(); i++) {
                wasmArgs[i] = wasmArgsList.get(i);
            }

            // Call the export function
            long[] results = exportFunction.apply(wasmArgs);

            // Decode and return the result
            if (returnTypes.isEmpty()) {
                return null;
            }

            WitType returnType = returnTypes.get(0);

            // Special handling for records: decode from pointer returned by WASM
            if (returnsRecord) {
                // For records, WASM returns a pointer to the record in memory
                if (results.length > 0 && results[0] != 0) {
                    Object decoded =
                            CanonicalAbi.decodeRecordFromMemory(
                                    results[0], returnRecordType, memory);
                    // Try to convert Map to POJO
                    if (decoded instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) decoded;
                        return PojoRegistry.mapToPojo(returnRecordType.displayName(), map);
                    }
                    return decoded;
                }
                return new java.util.HashMap<>();
            }

            // Special handling for variants
            if (returnsVariant) {
                Object variantResult = null;
                if (results.length > 0) {
                    long value = results[0];

                    // For wit-bindgen variants, try decoding as inline first
                    // (covers both simple variants and variants with inline data)
                    try {
                        variantResult =
                                CanonicalAbi.decodeVariantInline(
                                        new long[] {value}, returnVariantType, memory);
                    } catch (Exception e) {
                        // If inline decoding fails, try as memory pointer
                        // For variants with associated data, the memory layout may be non-standard.
                        // Try decoding each possible case and use the one that succeeds.
                        variantResult = tryDecodeVariantCases(value, returnVariantType, memory);
                    }
                } else {
                    variantResult = new VariantValue("", null);
                }

                // Try to convert to POJO if registered
                if (variantResult instanceof VariantValue) {
                    VariantValue vv = (VariantValue) variantResult;
                    Object converted =
                            PojoRegistry.variantValueToPojo(returnVariantType.displayName(), vv);
                    return converted;
                }
                return variantResult;
            }

            // Special handling for lists
            if (returnsList) {
                long sourcePtr = sretPtr > 0 ? sretPtr : (results.length >= 1 ? results[0] : 0);
                if (sourcePtr > 0) {
                    int listPtr = memory.readInt((int) sourcePtr);
                    int listCount = memory.readInt((int) sourcePtr + 4);
                    System.out.println(
                            "[DEBUG list] Read from 0x"
                                    + Long.toHexString(sourcePtr)
                                    + ": ptr=0x"
                                    + Integer.toHexString(listPtr)
                                    + ", count="
                                    + listCount);
                    if (listCount >= 0 && listPtr > 0) {
                        return CanonicalAbi.decode(
                                new long[] {listPtr, listCount}, returnListType, memory);
                    }
                }
                return new ListValue(new ArrayList<>());
            }

            // For strings and other types, results may contain multiple values
            return CanonicalAbi.decode(
                    Arrays.copyOf(results, Math.max(1, results.length)), returnType, memory);
        } finally {
            // Clear realloc context after encoding/decoding
            CanonicalAbi.clearContext();
        }
    }

    /**
     * Get the underlying export function.
     */
    public ExportFunction getExportFunction() {
        return exportFunction;
    }

    /**
     * Get the function signature.
     */
    public ComponentDefinition.FunctionSignature getSignature() {
        return signature;
    }

    /**
     * Convert a POJO to a Map by extracting fields using reflection.
     */
    private static Map<String, Object> pojoToMap(Object pojo) {
        Map<String, Object> map = new java.util.HashMap<>();

        if (pojo == null) {
            return map;
        }

        Class<?> clazz = pojo.getClass();

        // Extract all fields using reflection
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(pojo);
                map.put(field.getName(), fieldValue);
            } catch (IllegalAccessException e) {
                // Skip this field
            }
        }

        return map;
    }

    /**
     * Try to decode a variant by attempting to decode each possible case. For variants with
     * complex associated data, the memory layout may be non-standard (not starting with a
     * discriminant). This method tries to decode the data at the pointer location as each
     * variant case and returns the first one that succeeds.
     */
    private static Object tryDecodeVariantCases(
            long pointer, VariantType variantType, Memory memory) {
        java.util.List<VariantType.Case> cases = variantType.cases();

        // Try each case in order
        for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
            VariantType.Case caseInfo = cases.get(caseIndex);

            try {
                // Try to decode this case's data from memory
                Object caseData = null;

                if (caseInfo.type.isPresent()) {
                    // Attempt to read and decode case data
                    // For string types, try reading (ptr, len) pair at offset 4 and 8
                    WitType caseType = caseInfo.type.get();

                    if (caseType instanceof PrimitiveType) {
                        PrimitiveType primType = (PrimitiveType) caseType;
                        if ("string".equals(primType.name()) || "STRING".equals(primType.name())) {
                            // Try to read string at offset 4 (assuming (ptr, len) encoding)
                            int stringPtr = memory.readInt((int) pointer + 4);
                            int stringLen = memory.readInt((int) pointer + 8);

                            if (stringLen > 0 && stringLen < 100000) {
                                // Looks like a valid string
                                String str =
                                        memory.readString(
                                                stringPtr,
                                                stringLen,
                                                java.nio.charset.StandardCharsets.UTF_8);
                                caseData = str;
                                // Successfully decoded this case!
                                return new VariantValue(caseInfo.name, caseData);
                            }
                        } else {
                            // For other primitives, try to read from offset 4
                            switch (primType.name()) {
                                case "s32":
                                case "u32":
                                    int intVal = memory.readInt((int) pointer + 4);
                                    // Assume if we can read an int, this might be the case
                                    caseData = intVal;
                                    return new VariantValue(caseInfo.name, caseData);
                                case "s64":
                                case "u64":
                                    long longVal = memory.readLong((int) pointer + 4);
                                    caseData = longVal;
                                    return new VariantValue(caseInfo.name, caseData);
                            }
                        }
                    } else {
                        // Not a PrimitiveType, skip this case
                    }
                } else {
                    // Empty case (no associated data) - assume this case if no data found
                    if (pointer == 0 || pointer == caseIndex) {
                        return new VariantValue(caseInfo.name, null);
                    }
                }
            } catch (Exception e) {
                // This case didn't work, try the next one
            }
        }

        // If nothing worked, return empty variant
        return new VariantValue("", null);
    }
}
