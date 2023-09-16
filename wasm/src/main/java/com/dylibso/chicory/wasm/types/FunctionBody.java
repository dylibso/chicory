package com.dylibso.chicory.wasm.types;

import java.util.List;

public class FunctionBody {
    private List<Value> locals;
    private List<Instruction> instructions;

    public FunctionBody(List<Value> locals, List<Instruction> instructions) {
        this.locals = locals;
        this.instructions = instructions;
    }

    public List<Value> getLocals() {
        return locals;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Ast getAst() {
        var ast = new Ast();
        for (var i : instructions) {
            ast.addInstruction(i);
        }
        return ast;
    }
}
