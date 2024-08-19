package com.dylibso.chicory.wasm.types;

import java.util.List;

public final class FunctionBody {
    private final List<ValueType> locals;
    private final List<AnnotatedInstruction> instructions;

    public FunctionBody(List<ValueType> locals, List<AnnotatedInstruction> instructions) {
        this.locals = List.copyOf(locals);
        this.instructions = List.copyOf(instructions);
    }

    public List<ValueType> localTypes() {
        return locals;
    }

    public List<AnnotatedInstruction> instructions() {
        return instructions;
    }
}
