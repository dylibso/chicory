package com.dylibso.chicory.wasm.types;

import java.util.List;

public class FunctionBody {
    private final List<ValueType> locals;
    private final List<Instruction> instructions;
    private boolean initializedByElem = false;

    public FunctionBody(List<ValueType> locals, List<Instruction> instructions) {
        this.locals = List.copyOf(locals);
        this.instructions = List.copyOf(instructions);
    }

    public List<ValueType> localTypes() {
        return locals;
    }

    public List<Instruction> instructions() {
        return instructions;
    }

    public void setInitializedByElem(boolean v) {
        initializedByElem = v;
    }

    public boolean isInitializedByElem() {
        return initializedByElem;
    }
}
