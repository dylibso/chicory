package com.dylibso.chicory.wasm.types;

public class CodeSection extends Section {
    private FunctionBody[] functionBodies;

    public CodeSection(FunctionBody[] functionBodies) {
        super(SectionId.CODE);
        this.functionBodies = functionBodies;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies;
    }
}
