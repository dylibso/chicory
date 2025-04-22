package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Function Section in a WebAssembly module.
 * This section declares the type signature for each function defined within the module.
 * The actual function bodies are defined in the Code Section.
 */
public final class FunctionSection extends Section {
    private final List<Integer> typeIndices;

    private FunctionSection(List<Integer> typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = List.copyOf(typeIndices);
    }

    /**
     * Returns the type index for the function at the specified index.
     * This index refers to a type defined in the Type Section.
     *
     * @param idx the index of the function.
     * @return the type index associated with the function.
     */
    public int getFunctionType(int idx) {
        return typeIndices.get(idx);
    }

    /**
     * Returns the {@link FunctionType} for the function at the specified index,
     * by looking up the type index in the provided {@link TypeSection}.
     *
     * @param idx the index of the function.
     * @param typeSection the {@link TypeSection} containing the function type definitions.
     * @return the {@link FunctionType} for the specified function.
     */
    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        return typeSection.getType(getFunctionType(idx));
    }

    /**
     * Returns the number of functions declared in this section.
     * This count should match the number of function bodies in the Code Section.
     *
     * @return the count of functions.
     */
    public int functionCount() {
        return typeIndices.size();
    }

    /**
     * Creates a new builder for constructing a {@link FunctionSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link FunctionSection} instances.
     */
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

        /**
         * Constructs the {@link FunctionSection} instance from the added type indices.
         *
         * @return the built {@link FunctionSection}.
         */
        public FunctionSection build() {
            return new FunctionSection(typeIndices);
        }
    }

    /**
     * Compares this function section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code FunctionSection} with the same type indices, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this function section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(typeIndices);
    }
}
