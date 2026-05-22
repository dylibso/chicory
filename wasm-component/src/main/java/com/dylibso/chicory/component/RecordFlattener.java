package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.Memory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles flattening and unflattening of WIT record types.
 *
 * <p>wit-bindgen performs ABI lowering that flattens composite types into primitive parameters.
 * This utility converts between the high-level Map representation and the flattened parameter
 * list expected by WASM functions.
 *
 * <p>Example: record person { name: string, age: i32, active: bool }
 *
 * <p>High-level: Map{name="Alice", age=30, active=true}
 * Flattened: [namePtr, nameLen, 30, 1] (4 parameters)
 */
public class RecordFlattener {

    /**
     * Flatten a record (Map) into individual field parameters.
     *
     * <p>Converts a high-level record representation (Java Map) into a flattened array of long
     * values suitable for passing as individual WASM function parameters.
     *
     * <p>Each field is encoded independently:
     * - String fields expand to (ptr, len) pairs
     * - Primitive fields expand to single values
     * - Result order matches field definition order in RecordType
     *
     * @param record Java Map representing the record
     * @param recordType RecordType specification with field definitions
     * @param memory Guest linear memory (needed for string encoding)
     * @return Flattened array of long values (one or two per field)
     */
    public static long[] flattenRecord(
            Map<String, Object> record, RecordType recordType, Memory memory) {
        List<Long> flattened = new ArrayList<>();

        for (RecordType.Field field : recordType.fields()) {
            Object fieldValue = record.get(field.name);
            long[] encoded = CanonicalAbi.encode(fieldValue, field.type, memory);
            for (long val : encoded) {
                flattened.add(val);
            }
        }

        // Convert List<Long> to long[]
        long[] result = new long[flattened.size()];
        for (int i = 0; i < flattened.size(); i++) {
            result[i] = flattened.get(i);
        }
        return result;
    }

    /**
     * Unflatten individual field parameters back into a record (Map).
     *
     * <p>Reverses the flattening operation: takes a flat array of WASM function result values
     * and reconstructs the high-level record representation as a Java Map.
     *
     * <p>Example: [namePtr, nameLen, 30, 1] → Map{name="Alice", age=30, active=true}
     *
     * @param flatParams Flattened array of long values from WASM function return
     * @param recordType RecordType specification with field definitions
     * @param memory Guest linear memory (needed for string decoding)
     * @return Java Map with field names as keys
     */
    public static Map<String, Object> unflattenRecord(
            long[] flatParams, RecordType recordType, Memory memory) {
        Map<String, Object> record = new HashMap<>();
        int paramIndex = 0;

        for (RecordType.Field field : recordType.fields()) {
            int fieldSize = getParameterCount(field.type);
            long[] fieldEncoded = new long[fieldSize];
            for (int i = 0; i < fieldSize; i++) {
                fieldEncoded[i] = flatParams[paramIndex + i];
            }
            Object decodedValue = CanonicalAbi.decode(fieldEncoded, field.type, memory);
            record.put(field.name, decodedValue);
            paramIndex += fieldSize;
        }

        return record;
    }

    /**
     * Calculate how many WASM parameters a WIT type expands to.
     *
     * <p>This is needed to know how many elements from the flattened array correspond to each
     * field:
     * - Primitives (i32, i64, bool, etc): 1 parameter
     * - String: 2 parameters (ptr, len)
     * - Lists: 2 parameters (ptr, len)
     * - Records: sum of all field parameters
     *
     * @param witType WIT type to count
     * @return Number of parameters this type expands to
     */
    public static int getParameterCount(WitType witType) {
        if (witType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) witType;
            if (prim == PrimitiveType.STRING) {
                // Strings expand to (ptr, len) = 2 parameters
                return 2;
            }
            // All other primitives are 1 parameter
            return 1;
        }

        // Lists expand to (ptr, len) = 2 parameters
        if (witType instanceof ListType) {
            return 2;
        }

        // Records: count all field parameters recursively
        if (witType instanceof RecordType) {
            RecordType rec = (RecordType) witType;
            int count = 0;
            for (RecordType.Field field : rec.fields()) {
                count += getParameterCount(field.type);
            }
            return count;
        }

        // Default: treat as 1 parameter
        return 1;
    }
}
