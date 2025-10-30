package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public final class CompType {
    private final ArrayType arrayType;
    private final StructType structType;
    private final FunctionType funcType;

    private CompType(ArrayType arrayType, StructType structType, FunctionType functionType) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ArrayType arrayType;
        private StructType structType;
        private FunctionType funcType;

        private Builder() {}

        public Builder withArrayType(ArrayType arrayType) {
            this.arrayType = arrayType;
            return this;
        }

        public Builder withStructType(StructType structType) {
            this.structType = structType;
            return this;
        }

        public Builder withFuncType(FunctionType funcType) {
            this.funcType = funcType;
            return this;
        }

        public CompType build() {
            return new CompType(arrayType, structType, funcType);
        }
    }
}
