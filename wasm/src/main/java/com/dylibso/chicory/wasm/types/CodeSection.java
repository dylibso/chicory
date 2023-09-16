package com.dylibso.chicory.wasm.types;

public class CodeSection extends Section {
    private FunctionBody[] functionBodies;

    public CodeSection(long id, long size, FunctionBody[] functionBodies) {
       super(id, size);
       this.functionBodies = functionBodies;
    }

    public FunctionBody[] getFunctionBodies() { return functionBodies; }
}
