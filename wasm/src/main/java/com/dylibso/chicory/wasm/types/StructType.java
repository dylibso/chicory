package com.dylibso.chicory.wasm.types;

import java.util.List;

public class StructType implements RecType {
    private final List<ValType> fieldTypes;

    // TODO: use a builder instead
    public StructType(List<ValType> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public List<ValType> getFieldTypes() {
        return fieldTypes;
    }
}
