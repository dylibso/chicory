package com.dylibso.chicory.wasm.types;

import java.util.Objects;
import java.util.function.Function;

public class CompType {
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
        private ArrayType.Builder arrayTypeBuilder;
        private StructType.Builder structTypeBuilder;
        private FunctionType.Builder funcTypeBuilder;

        private Builder() {}

        public Builder withArrayType(ArrayType.Builder arrayTypeBuilder) {
            this.arrayTypeBuilder = arrayTypeBuilder;
            return this;
        }

        public Builder withStructType(StructType.Builder structTypeBuilder) {
            this.structTypeBuilder = structTypeBuilder;
            return this;
        }

        public Builder withFuncType(FunctionType.Builder funcTypeBuilder) {
            this.funcTypeBuilder = funcTypeBuilder;
            return this;
        }

        public CompType build(Function<Integer, RecType> context) {
            return new CompType(
                    (arrayTypeBuilder == null) ? null : arrayTypeBuilder.build(context),
                    (structTypeBuilder == null) ? null : structTypeBuilder.build(context),
                    (funcTypeBuilder == null) ? null : funcTypeBuilder.build(context));
        }
    }
}
