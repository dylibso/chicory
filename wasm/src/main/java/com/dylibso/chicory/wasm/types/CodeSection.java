package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Code Section of a WebAssembly module.
 * This section contains the actual executable code (as sequences of instructions)
 * for each function defined within the module.
 */
public final class CodeSection extends Section {
    private final List<FunctionBody> functionBodies;
    private final boolean requiresDataCount;

    /**
     * Constructs a new Code Section.
     *
     * @param functionBodies the list of function bodies (must not be {@code null})
     * @param requiresDataCount true if any instruction in this section requires the DataCount section to be present
     */
    private CodeSection(List<FunctionBody> functionBodies, boolean requiresDataCount) {
        super(SectionId.CODE);
        this.functionBodies = List.copyOf(functionBodies);
        this.requiresDataCount = requiresDataCount;
    }

    /**
     * Returns the function bodies defined in this code section.
     *
     * @return an array of {@link FunctionBody} instances.
     */
    public FunctionBody[] functionBodies() {
        return functionBodies.toArray(new FunctionBody[0]);
    }

    /**
     * Returns the number of function bodies in this code section.
     *
     * @return the count of function bodies.
     */
    public int functionBodyCount() {
        return functionBodies.size();
    }

    /**
     * Returns the function body at the specified index.
     *
     * @param idx the index of the function body to retrieve.
     * @return the {@link FunctionBody} at the given index.
     */
    public FunctionBody getFunctionBody(int idx) {
        return functionBodies.get(idx);
    }

    /**
     * Indicates whether any instruction in this code section requires the DataCount section.
     *
     * @return {@code true} if the DataCount section is required, {@code false} otherwise.
     */
    public boolean isRequiresDataCount() {
        return requiresDataCount;
    }

    /**
     * Creates a new builder for constructing a {@link CodeSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link CodeSection} instances.
     */
    public static final class Builder {
        private final List<FunctionBody> functionBodies = new ArrayList<>();
        private boolean requiresDataCount;

        private Builder() {}

        /**
         * Adds a function body to this code section.
         *
         * @param functionBody the {@link FunctionBody} to add (must not be {@code null}).
         * @return this builder instance.
         */
        public Builder addFunctionBody(FunctionBody functionBody) {
            Objects.requireNonNull(functionBody, "functionBody");
            functionBodies.add(functionBody);
            return this;
        }

        /**
         * Sets the flag indicating whether this code section requires a DataCount section.
         * This should be set by the parser if it encounters instructions like
         * {@code memory.init} or {@code data.drop}.
         *
         * @param requiresDataCount true if required, false otherwise.
         * @return this builder instance.
         */
        public Builder setRequiresDataCount(boolean requiresDataCount) {
            this.requiresDataCount = requiresDataCount;
            return this;
        }

        /**
         * Constructs the {@link CodeSection} instance.
         *
         * @return the built {@link CodeSection}.
         */
        public CodeSection build() {
            return new CodeSection(functionBodies, requiresDataCount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof CodeSection)) {
            return false;
        }
        CodeSection that = (CodeSection) o;
        return requiresDataCount == that.requiresDataCount
                && Objects.equals(functionBodies, that.functionBodies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionBodies, requiresDataCount);
    }
}
