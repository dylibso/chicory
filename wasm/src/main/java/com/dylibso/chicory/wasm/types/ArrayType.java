package com.dylibso.chicory.wasm.types;

import java.util.function.Function;

public class ArrayType {
    private final FieldType fieldType;

    private ArrayType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType fieldType() {
        return fieldType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private FieldType.Builder fieldTypeBuilder;

        private Builder() {}

        public Builder withFieldTypeBuilder(FieldType.Builder fieldTypeBuilder) {
            this.fieldTypeBuilder = fieldTypeBuilder;
            return this;
        }

        public ArrayType build(Function<Integer, RecType> context) {
            return new ArrayType(fieldTypeBuilder.build(context));
        }
    }
}
