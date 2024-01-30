package com.dylibso.chicory.wasm.types;

public class FunctionBody {
    private final ValueType[] locals;
    private final Instruction[] instructions;

    public FunctionBody(ValueType[] locals, Instruction[] instructions) {
        this.locals = locals;
        this.instructions = instructions;
    }

    public ValueType[] localTypes() {
        return locals;
    }

    public Instruction[] instructions() {
        return instructions;
    }
}
