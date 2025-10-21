package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionSection extends Section {
    private final List<Integer> typeIndices;

    private FunctionSection(List<Integer> typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = List.copyOf(typeIndices);
    }

    public int getFunctionType(int idx) {
        return typeIndices.get(idx);
    }

    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        // TODO: this is based on a lot of assumptions ..
        return typeSection.getType(getFunctionType(idx)).subTypes()[0].compType().funcType();
    }

    public int functionCount() {
        return typeIndices.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Integer> typeIndices = new ArrayList<>();

        private Builder() {}

        /**
         * Add a function type index to this section.
         *
         * @param typeIndex the type index to add (should be a valid index into the type section)
         * @return the Builder
         */
        public Builder addFunctionType(int typeIndex) {
            typeIndices.add(typeIndex);
            return this;
        }

        public FunctionSection build() {
            return new FunctionSection(typeIndices);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof FunctionSection)) {
            return false;
        }
        FunctionSection that = (FunctionSection) o;
        return Objects.equals(typeIndices, that.typeIndices);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeIndices);
    }
}
