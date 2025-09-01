package com.dylibso.chicory.wasm.types;

public class ArrayType implements RecType {
    private final ValType fieldType;

    // TODO: use a builder
    public ArrayType(ValType fieldType) {
        this.fieldType = fieldType;
    }

    public ValType getFieldType() {
        return fieldType;
    }
}
