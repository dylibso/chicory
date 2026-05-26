package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.component.types.WitType;
import java.util.Optional;

/**
 * Parses WIT types from Component Model binary format.
 *
 * <p>The binary format encodes types using a kind byte followed by type-specific data:
 * - 0x7F: i32
 * - 0x7E: i64
 * - 0x7D: f32
 * - 0x7C: f64
 * - 0x01: bool
 * - 0x02: char
 * - 0x03: string
 * - 0x04: list (followed by element type)
 * - 0x05: record (followed by field count + field definitions)
 * - 0x06: variant (followed by case count + case definitions)
 * - Other: type index reference
 */
public class BinaryTypeParser {

    // Primitive type discriminators
    private static final byte TYPE_I32 = 0x7F;
    private static final byte TYPE_I64 = 0x7E;
    private static final byte TYPE_F32 = 0x7D;
    private static final byte TYPE_F64 = 0x7C;
    private static final byte TYPE_BOOL = 0x01;
    private static final byte TYPE_CHAR = 0x02;
    private static final byte TYPE_STRING = 0x03;
    private static final byte TYPE_LIST = 0x04;
    private static final byte TYPE_RECORD = 0x05;
    private static final byte TYPE_VARIANT = 0x06;

    /**
     * Parse a type from binary format.
     *
     * <p>Types can be:
     * 1. Primitive types (encoded as specific bytes)
     * 2. String (0x03)
     * 3. Container types like list, record, variant (have nested type info)
     * 4. Type references (indices into type table)
     *
     * @param reader binary reader
     * @param typeRegistry for looking up named types
     * @return parsed WIT type
     */
    public static WitType parseType(BinaryComponentReader reader, ComponentDefinition definition)
            throws ComponentModelException {
        byte typeByte = reader.readByte();

        // Check for primitive types
        switch (typeByte) {
            case TYPE_I32:
                return PrimitiveType.I32;
            case TYPE_I64:
                return PrimitiveType.I64;
            case TYPE_F32:
                return PrimitiveType.F32;
            case TYPE_F64:
                return PrimitiveType.F64;
            case TYPE_BOOL:
                return PrimitiveType.BOOL;
            case TYPE_CHAR:
                return PrimitiveType.CHAR;
            case TYPE_STRING:
                return PrimitiveType.STRING;
            case TYPE_LIST:
                return parseList(reader, definition);
            case TYPE_RECORD:
                return parseRecord(reader, definition);
            case TYPE_VARIANT:
                return parseVariant(reader, definition);
            default:
                // Could be a type reference or named type
                // For now, treat as a reference that needs resolution
                // Put byte back for unsigned read
                reader.skip(-1);
                int typeIndex = reader.readTypeIndex();
                // TODO: Resolve type reference from type table
                // For MVP, return a placeholder
                return PrimitiveType.I32; // Temporary fallback
        }
    }

    /**
     * Parse a list type from binary.
     *
     * <p>Format: element_type
     *
     * @param reader binary reader
     * @param definition component definition
     * @return ListType
     */
    private static WitType parseList(BinaryComponentReader reader, ComponentDefinition definition)
            throws ComponentModelException {
        WitType elementType = parseType(reader, definition);
        return new ListType(elementType);
    }

    /**
     * Parse a record type from binary.
     *
     * <p>Format: field_count (field_name field_type)*
     *
     * @param reader binary reader
     * @param definition component definition
     * @return RecordType
     */
    private static WitType parseRecord(BinaryComponentReader reader, ComponentDefinition definition)
            throws ComponentModelException {
        // For MVP, generate a synthetic record name
        String recordName = "Record_" + System.identityHashCode(reader);
        RecordType record = new RecordType(recordName);

        long fieldCount = reader.readUnsigned();
        if (fieldCount < 0 || fieldCount > 1000) { // sanity check
            throw new ComponentModelException("Record field count too large: " + fieldCount);
        }

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = reader.readString();
            WitType fieldType = parseType(reader, definition);
            record.addField(fieldName, fieldType);
        }

        return record;
    }

    /**
     * Parse a variant type from binary.
     *
     * <p>Format: case_count (case_name [case_type])*
     *
     * @param reader binary reader
     * @param definition component definition
     * @return VariantType
     */
    private static WitType parseVariant(
            BinaryComponentReader reader, ComponentDefinition definition)
            throws ComponentModelException {
        // For MVP, generate a synthetic variant name
        String variantName = "Variant_" + System.identityHashCode(reader);
        VariantType variant = new VariantType(variantName);

        long caseCount = reader.readUnsigned();
        if (caseCount < 0 || caseCount > 1000) { // sanity check
            throw new ComponentModelException("Variant case count too large: " + caseCount);
        }

        for (int i = 0; i < caseCount; i++) {
            String caseName = reader.readString();
            // Check if case has associated type
            byte hasType = reader.readByte();
            if (hasType != 0) {
                // Has type - need to put byte back for parseType
                reader.skip(-1);
                WitType caseType = parseType(reader, definition);
                variant.addCase(caseName, Optional.of(caseType));
            } else {
                // No type - just tag
                variant.addCase(caseName, Optional.empty());
            }
        }

        return variant;
    }

    /**
     * Parse a function signature type from binary.
     *
     * <p>Format: param_count (param_name param_type)* return_count (return_type)*
     *
     * @param reader binary reader
     * @param definition component definition
     * @return component definition function signature
     */
    public static ComponentDefinition.FunctionSignature parseFunction(
            BinaryComponentReader reader, ComponentDefinition definition)
            throws ComponentModelException {
        String functionName = reader.readString();
        ComponentDefinition.FunctionSignature sig =
                new ComponentDefinition.FunctionSignature(functionName);

        // Parse parameters
        long paramCount = reader.readUnsigned();
        if (paramCount < 0 || paramCount > 1000) {
            throw new ComponentModelException("Function param count too large: " + paramCount);
        }

        for (int i = 0; i < paramCount; i++) {
            String paramName = reader.readString();
            WitType paramType = parseType(reader, definition);
            sig.addParameter(paramName, paramType);
        }

        // Parse return types
        long returnCount = reader.readUnsigned();
        if (returnCount < 0 || returnCount > 1000) {
            throw new ComponentModelException("Function return count too large: " + returnCount);
        }

        for (int i = 0; i < returnCount; i++) {
            WitType returnType = parseType(reader, definition);
            sig.addReturnType(returnType);
        }

        return sig;
    }
}
