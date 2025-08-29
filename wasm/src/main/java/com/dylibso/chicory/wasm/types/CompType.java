package com.dylibso.chicory.wasm.types;

public class CompType implements RecType {
    private final ArrayType arrayType;
    private final StructType structType;
    private final FunctionType functionType;

    // TODO: move to a builder
    public CompType(ArrayType arrayType, StructType structType, FunctionType functionType) {
        this.arrayType = arrayType;
        this.structType = structType;
        this.functionType = functionType;
    }
}
