package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public final class StructType {
    private final FieldType[] fieldTypes;

    private StructType(FieldType[] fieldTypes) {
        this.fieldTypes = fieldTypes.clone();
    }

    public FieldType[] fieldTypes() {
        return fieldTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<FieldType> fieldTypes = new ArrayList<>();

        private Builder() {}

        public Builder addFieldType(FieldType fieldType) {
            this.fieldTypes.add(fieldType);
            return this;
        }

        public StructType build() {
            return new StructType(fieldTypes.toArray(FieldType[]::new));
        }
    }
}
