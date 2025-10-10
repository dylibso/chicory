package com.dylibso.chicory.wasm.types;

public class ArrayType {
    private final FieldType fieldType;

    public ArrayType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType fieldType() {
        return fieldType;
    }
}
