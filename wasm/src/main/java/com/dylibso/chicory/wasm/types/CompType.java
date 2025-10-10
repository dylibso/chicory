package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public class CompType {
    private final ArrayType arrayType;
    private final StructType structType;
    private final FunctionType funcType;

    // TODO: builders
    public CompType(ArrayType arrayType, StructType structType, FunctionType functionType) {
        this.arrayType = arrayType;
        this.structType = structType;
        this.funcType = functionType;
    }

    public FunctionType funcType() {
        return funcType;
    }

    public StructType structType() {
        return structType;
    }

    public ArrayType arrayType() {
        return arrayType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompType compType = (CompType) o;
        return Objects.equals(arrayType, compType.arrayType)
                && Objects.equals(structType, compType.structType)
                && Objects.equals(funcType, compType.funcType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayType, structType, funcType);
    }
}
