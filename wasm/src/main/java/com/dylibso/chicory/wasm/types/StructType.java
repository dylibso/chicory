package com.dylibso.chicory.wasm.types;

import java.util.List;

public class StructType implements RecType {
    private final List<FieldType> fieldTypes;

    // TODO: use a builder instead
    public StructType(List<FieldType> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public List<FieldType> getFieldTypes() {
        return fieldTypes;
    }
}
