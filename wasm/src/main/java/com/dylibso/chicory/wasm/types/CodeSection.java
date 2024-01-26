package com.dylibso.chicory.wasm.types;

public class CodeSection extends Section {
    private FunctionBody[] functionBodies;

    public CodeSection(long id, FunctionBody[] functionBodies) {
        super(id);
        this.functionBodies = functionBodies;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies;
    }
}
