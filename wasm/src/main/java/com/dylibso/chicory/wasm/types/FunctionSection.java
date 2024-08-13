package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class FunctionSection extends Section {
    private final List<Integer> typeIndices;

    private FunctionSection(List<Integer> typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = List.copyOf(typeIndices);
    }

    public int getFunctionType(int idx) {
        return typeIndices.get(idx);
    }

    public int functionCount() {
        return typeIndices.size();
    }

    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        return typeSection.getType(getFunctionType(idx));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Integer> typeIndices = new ArrayList<>();

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
}
