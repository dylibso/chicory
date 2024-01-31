package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class CodeSection extends Section {
    private final ArrayList<FunctionBody> functionBodies;

    private CodeSection(ArrayList<FunctionBody> functionBodies) {
        super(SectionId.CODE);
        this.functionBodies = functionBodies;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(CodeSection codeSection) {
        return new Builder(codeSection);
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

    public static final class Builder {

        private final ArrayList<FunctionBody> functionBodies;

        private Builder() {
            this.functionBodies = new ArrayList<>();
        }

        private Builder(CodeSection codeSection) {
            this.functionBodies = new ArrayList<>();
            this.functionBodies.addAll(codeSection.functionBodies);
        }

        public Builder addFunctionBody(FunctionBody functionBody) {
            Objects.requireNonNull(functionBody, "functionBody");
            functionBodies.add(functionBody);
            return this;
        }

        public CodeSection build() {
            return new CodeSection(functionBodies);
        }
    }
}
