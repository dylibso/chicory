package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.WitType;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates memory layout for records following canonical ABI alignment rules.
 *
 * Canonical ABI alignment:
 * - i32, f32: 4-byte aligned
 * - i64, f64: 8-byte aligned
 * - bool, char: 1-byte aligned
 * - string: 8-byte aligned (two i32s form pointer pair)
 * - record/variant: aligned to largest field alignment
 *
 * Layout strategy:
 * - Fields laid out sequentially in declaration order
 * - Each field aligned to its type's requirement
 * - Padding added as needed between fields
 * - Final record size aligned to largest field alignment
 */
public class RecordLayout {
    private final RecordType recordType;
    private final List<FieldLayout> fieldLayouts;
    private final int recordAlignment;
    private final int recordSize;

    /**
     * Calculate memory layout for a record type.
     *
     * @param recordType WIT record type
     */
    public RecordLayout(RecordType recordType) {
        this.recordType = recordType;
        this.fieldLayouts = new ArrayList<>();

        int maxAlignment = 1;
        int currentOffset = 0;

        // Process each field in declaration order
        for (RecordType.Field field : recordType.fields()) {
            int alignment = getTypeAlignment(field.type);
            maxAlignment = Math.max(maxAlignment, alignment);

            // Add padding if needed to align to field's requirement
            if (currentOffset % alignment != 0) {
                int padding = alignment - (currentOffset % alignment);
                currentOffset += padding;
            }

            FieldLayout fieldLayout =
                    new FieldLayout(field.name, field.type, currentOffset, alignment);
            fieldLayouts.add(fieldLayout);

            // Move offset past this field
            int fieldSize = getTypeSize(field.type);
            currentOffset += fieldSize;
        }

        // Align final size to record's alignment
        if (currentOffset % maxAlignment != 0) {
            int padding = maxAlignment - (currentOffset % maxAlignment);
            currentOffset += padding;
        }

        this.recordAlignment = maxAlignment;
        this.recordSize = currentOffset;
    }

    /**
     * Get the alignment requirement for a type.
     *
     * @param type WIT type
     * @return Alignment in bytes
     */
    public static int getTypeAlignment(WitType type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) type;
            switch (prim) {
                case I32:
                case F32:
                    return 4;
                case I64:
                case F64:
                    return 8;
                case STRING:
                    return 8; // (ptr, len) pair = two i32s = 8 bytes
                case BOOL:
                case CHAR:
                    return 1;
                default:
                    return 1;
            }
        }

        // Composite types: align to max field alignment
        // (This would be computed recursively for nested types)
        return 1; // Default; improve for nested types
    }

    /**
     * Get the size in bytes for a type.
     *
     * @param type WIT type
     * @return Size in bytes
     */
    public static int getTypeSize(WitType type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) type;
            switch (prim) {
                case I32:
                case F32:
                    return 4;
                case I64:
                case F64:
                    return 8;
                case STRING:
                    return 8; // (ptr, len) pair = two i32s = 8 bytes
                case BOOL:
                case CHAR:
                    return 1;
                default:
                    return 1;
            }
        }

        // For composite types, size would be calculated from fields
        return 1; // Placeholder
    }

    /**
     * Calculate the total size needed to store a record in memory.
     * Convenience method for statically calculating record size without creating a layout object.
     *
     * @param recordType Record type
     * @return Size in bytes needed to store the record
     */
    public static int calculateRecordSize(RecordType recordType) {
        return new RecordLayout(recordType).getRecordSize();
    }

    // Getters
    public int getRecordSize() {
        return recordSize;
    }

    public int getRecordAlignment() {
        return recordAlignment;
    }

    public List<FieldLayout> getFieldLayouts() {
        return fieldLayouts;
    }

    public FieldLayout getField(String fieldName) {
        return fieldLayouts.stream()
                .filter(f -> f.name.equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldName));
    }

    /**
     * Information about a field's position and alignment.
     */
    public static class FieldLayout {
        public final String name;
        public final WitType type;
        public final int offset;
        public final int alignment;

        public FieldLayout(String name, WitType type, int offset, int alignment) {
            this.name = name;
            this.type = type;
            this.offset = offset;
            this.alignment = alignment;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Record Layout for ").append(recordType.displayName()).append(":\n");
        sb.append("  Size: ").append(recordSize).append(" bytes\n");
        sb.append("  Alignment: ").append(recordAlignment).append(" bytes\n");
        for (FieldLayout field : fieldLayouts) {
            sb.append("  - ")
                    .append(field.name)
                    .append(" @ offset ")
                    .append(field.offset)
                    .append(" (")
                    .append(field.type.displayName())
                    .append(")\n");
        }
        return sb.toString();
    }
}
