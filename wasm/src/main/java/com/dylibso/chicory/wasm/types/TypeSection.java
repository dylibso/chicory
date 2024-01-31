package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class TypeSection extends Section {
    private final ArrayList<FunctionType> types;

    private TypeSection(ArrayList<FunctionType> types) {
        super(SectionId.TYPE);
        this.types = types;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TypeSection typeSection) {
        return new Builder(typeSection);
    }

    public FunctionType[] types() {
        return types.toArray(FunctionType[]::new);
    }

    public int typeCount() {
        return types.size();
    }

    public FunctionType getType(int idx) {
        return types.get(idx);
    }

    public static final class Builder {
        private final ArrayList<FunctionType> types;

        private Builder() {
            this.types = new ArrayList<>();
        }

        private Builder(TypeSection typeSection) {
            this.types = new ArrayList<>();
            this.types.addAll(typeSection.types);
        }

        public Builder addFunctionType(FunctionType functionType) {
            Objects.requireNonNull(functionType, "functionType");
            types.add(functionType);
            return this;
        }

        public TypeSection build() {
            return new TypeSection(types);
        }
    }
}
