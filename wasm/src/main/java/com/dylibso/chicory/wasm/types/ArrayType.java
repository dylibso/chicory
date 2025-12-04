package com.dylibso.chicory.wasm.types;

public final class ArrayType {
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
        private FieldType fieldType;

        private Builder() {}

        public Builder withFieldType(FieldType fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public ArrayType build() {
            return new ArrayType(fieldType);
        }
    }
}
