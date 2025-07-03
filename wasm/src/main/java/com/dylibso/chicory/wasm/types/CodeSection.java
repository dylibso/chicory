package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CodeSection extends Section {
    private final List<FunctionBody> functionBodies;
    private final boolean requiresDataCount;
    private final int address;

    private CodeSection(List<FunctionBody> functionBodies, boolean requiresDataCount, int address) {
        super(SectionId.CODE);
        this.functionBodies = List.copyOf(functionBodies);
        this.requiresDataCount = requiresDataCount;
        this.address = address;
    }

    public int address() {
        return address;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies.toArray(new FunctionBody[0]);
    }

    public int functionBodyCount() {
        return functionBodies.size();
    }

    public FunctionBody getFunctionBody(int idx) {
        return functionBodies.get(idx);
    }

    public boolean isRequiresDataCount() {
        return requiresDataCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<FunctionBody> functionBodies = new ArrayList<>();
        private boolean requiresDataCount;
        private int address;

        private Builder() {}

        /**
         * Add a function body to this section.
         *
         * @param functionBody the function body to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addFunctionBody(FunctionBody functionBody) {
            Objects.requireNonNull(functionBody, "functionBody");
            functionBodies.add(functionBody);
            return this;
        }

        public Builder setRequiresDataCount(boolean requiresDataCount) {
            this.requiresDataCount = requiresDataCount;
            return this;
        }

        public CodeSection build() {
            return new CodeSection(functionBodies, requiresDataCount, address);
        }

        public Builder withSectionAddress(int sectionAddress) {
            this.address = sectionAddress;
            return this;
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
