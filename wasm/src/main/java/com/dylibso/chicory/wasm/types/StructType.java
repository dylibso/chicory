package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StructType {
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
        private List<FieldType.Builder> fieldTypeBuilder = new ArrayList<>();

        private Builder() {}

        public Builder addFieldTypeBuilder(FieldType.Builder fieldTypeBuilder) {
            this.fieldTypeBuilder.add(fieldTypeBuilder);
            return this;
        }

        public StructType build(Function<Integer, RecType> context) {
            var fieldTypes = new FieldType[fieldTypeBuilder.size()];
            for (int i = 0; i < fieldTypeBuilder.size(); i++) {
                fieldTypes[i] = fieldTypeBuilder.get(i).build(context);
            }
            return new StructType(fieldTypes);
        }
    }
}
