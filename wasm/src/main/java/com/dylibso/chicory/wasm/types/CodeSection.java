package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeSection extends Section {
    private final List<FunctionBody> functionBodies;
    private final boolean requiresDataCount;

    private CodeSection(List<FunctionBody> functionBodies, boolean requiresDataCount) {
        super(SectionId.CODE);
        this.functionBodies = List.copyOf(functionBodies);
        this.requiresDataCount = requiresDataCount;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies.toArray(FunctionBody[]::new);
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

    public static class Builder {
        private List<FunctionBody> functionBodies = new ArrayList<>();
        private boolean requiresDataCount = false;

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
            return new CodeSection(functionBodies, requiresDataCount);
        }
    }
}
