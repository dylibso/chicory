package com.dylibso.chicory.wasm.types;

public class ArrayType implements RecType {
    private final FieldType fieldType;

    // TODO: use a builder
    public ArrayType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType getFieldType() {
        return fieldType;
    }
}
