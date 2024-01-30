package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class CodeSection extends Section {
    private final ArrayList<FunctionBody> functionBodies;

    public CodeSection(FunctionBody[] functionBodies) {
        super(SectionId.CODE);
        this.functionBodies = new ArrayList<>(List.of(functionBodies));
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
}
