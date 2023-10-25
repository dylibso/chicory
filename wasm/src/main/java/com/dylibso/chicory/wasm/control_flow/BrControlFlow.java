package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;

public class BrControlFlow implements Loopable, Endable {

    private Instruction instruction;
    private int offset = 0;
    private boolean hasEnd = false;

    public BrControlFlow(Instruction instruction, int i) {
        this.instruction = instruction;
        this.offset = (int) instruction.getOperands()[0];
        instruction.setLabelTrue(i + 1);
        instruction.setLabelFalse(i + 1);
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public void onEnd(Instruction end, int i) {
        if (end.getScope() == OpCode.LOOP) {
            // throw new RuntimeException("Implement this logic");
        } else {
            instruction.setLabelTrue(i);
        }
    }

    @Override
    public void onLoop(Instruction loop, int i) {
        throw new RuntimeException("Fix me");
    }
}
