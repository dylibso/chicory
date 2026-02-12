package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

public final class FunctionBody {
    private final List<ValType> locals;
    private final List<AnnotatedInstruction> instructions;

    public FunctionBody(List<ValType> locals, List<AnnotatedInstruction> instructions) {
        this.locals = List.copyOf(locals);
        this.instructions = List.copyOf(instructions);
    }

    public List<ValType> localTypes() {
        return locals;
    }

    public List<AnnotatedInstruction> instructions() {
        return instructions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof FunctionBody)) {
            return false;
        }
        FunctionBody that = (FunctionBody) o;
        return Objects.equals(locals, that.locals)
                && Objects.equals(instructions, that.instructions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locals, instructions);
    }
}
