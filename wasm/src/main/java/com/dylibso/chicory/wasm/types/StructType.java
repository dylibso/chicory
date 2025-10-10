package com.dylibso.chicory.wasm.types;

public class StructType {
    private final FieldType[] fieldTypes;

    public StructType(FieldType[] fieldTypes) {
        this.fieldTypes = fieldTypes.clone();
    }

    public FieldType[] fieldTypes() {
        return fieldTypes;
    }
}
