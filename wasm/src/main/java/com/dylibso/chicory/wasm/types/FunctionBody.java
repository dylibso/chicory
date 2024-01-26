package com.dylibso.chicory.wasm.types;

import java.util.List;

public class FunctionBody {
    private final ValueType[] locals;
    private List<Instruction> instructions;

    public FunctionBody(ValueType[] locals, List<Instruction> instructions) {
        this.locals = locals;
        this.instructions = instructions;
    }

    public ValueType[] locals() {
        return locals;
    }

    public List<Instruction> instructions() {
        return instructions;
    }

    public Ast ast() {
        var ast = new Ast();
        for (var i : instructions) {
            ast.addInstruction(i);
        }
        return ast;
    }
}
