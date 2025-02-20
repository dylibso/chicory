package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeSection extends Section {
    private final List<FunctionType> types;

    private TypeSection(List<FunctionType> types) {
        super(SectionId.TYPE);
        this.types = List.copyOf(types);
    }

    public FunctionType[] types() {
        return types.toArray(new FunctionType[0]);
    }

    public int typeCount() {
        return types.size();
    }

    public FunctionType getType(int idx) {
        return types.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<FunctionType> types = new ArrayList<>();

        private Builder() {}

        /**
         * Add a function type definition to this section.
         *
         * @param functionType the function type to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addFunctionType(FunctionType functionType) {
            Objects.requireNonNull(functionType, "functionType");
            types.add(functionType);
            return this;
        }

        public TypeSection build() {
            return new TypeSection(types);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof TypeSection)) {
            return false;
        }
        TypeSection that = (TypeSection) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(types);
    }
}
