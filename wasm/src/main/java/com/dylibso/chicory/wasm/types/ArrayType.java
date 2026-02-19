package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public final class ArrayType {
    private final FieldType fieldType;

    private ArrayType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType fieldType() {
        return fieldType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(fieldType, arrayType.fieldType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fieldType);
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
