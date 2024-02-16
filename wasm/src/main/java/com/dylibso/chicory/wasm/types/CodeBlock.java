package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class CodeBlock {
    private BlockKind kind;
    private List<Instruction> instructions;

    public CodeBlock(BlockKind kind) {
        this.kind = kind;
        this.instructions = new ArrayList<>();
    }

    public void addInstruction(Instruction i) {
        this.instructions.add(i);
    }

    public List<Instruction> instructions() {
        return instructions;
    }
}
