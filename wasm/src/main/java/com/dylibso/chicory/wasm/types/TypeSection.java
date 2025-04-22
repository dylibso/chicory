package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Type Section in a WebAssembly module.
 * This section declares all function types (signatures) used within the module.
 */
public final class TypeSection extends Section {
    private final List<FunctionType> types;

    private TypeSection(List<FunctionType> types) {
        super(SectionId.TYPE);
        this.types = List.copyOf(types);
    }

    /**
     * Returns the function types defined in this section.
     *
     * @return an array of {@link FunctionType} instances.
     */
    public FunctionType[] types() {
        return types.toArray(new FunctionType[0]);
    }

    /**
     * Returns the number of function types defined in this section.
     *
     * @return the count of function types.
     */
    public int typeCount() {
        return types.size();
    }

    /**
     * Returns the function type at the specified index.
     *
     * @param idx the index of the function type to retrieve.
     * @return the {@link FunctionType} at the given index.
     */
    public FunctionType getType(int idx) {
        return types.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link TypeSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link TypeSection} instances.
     */
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

        /**
         * Constructs the {@link TypeSection} instance from the added function types.
         *
         * @return the built {@link TypeSection}.
         */
        public TypeSection build() {
            return new TypeSection(types);
        }
    }

    /**
     * Compares this type section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code TypeSection} with the same function types, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this type section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(types);
    }
}
