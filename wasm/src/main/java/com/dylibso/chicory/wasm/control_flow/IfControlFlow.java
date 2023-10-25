package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;

public class IfControlFlow implements Elseable, Endable {
    private Instruction ifInstruction;
    private Instruction elseInstruction;

    public IfControlFlow(Instruction ifInstruction, int i) {
        this.ifInstruction = ifInstruction;
        // Let's start setting the next instruction
        ifInstruction.setLabelTrue(i + 1);
        ifInstruction.setLabelFalse(i + 1);
    }

    public void onElse(Instruction elze, int i) {
        // change the next false label to the next instruction
        ifInstruction.setLabelFalse(i + 1);
        elseInstruction = elze;
    }

    public void onEnd(Instruction end, int i) {
        if (elseInstruction == null) {
            // change the next false label to the next instruction
            ifInstruction.setLabelFalse(i);
        } else {
            elseInstruction.setLabelTrue(i);
        }
    }
}
