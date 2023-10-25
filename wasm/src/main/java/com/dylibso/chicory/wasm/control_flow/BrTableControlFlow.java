package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;

public class BrTableControlFlow implements Loopable, Endable {

    private Instruction instruction;
    private int idx;
    private int offset = 0;

    public BrTableControlFlow(Instruction instruction, int i, int idx) {
        this.instruction = instruction;
        this.idx = idx;
        this.offset = (int) instruction.getOperands()[idx];
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public void onEnd(Instruction end, int i) {
        if (end.getScope() == OpCode.LOOP) {
            // throw new RuntimeException("Implement this logic");
        } else {
            instruction.getLabelTable()[idx] = i + 1;
        }
    }

    @Override
    public void onLoop(Instruction loop, int i) {
        throw new RuntimeException("Fix me");
    }
}
