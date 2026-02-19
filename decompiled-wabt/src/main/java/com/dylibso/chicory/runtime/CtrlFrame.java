package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.OpCode;

final class CtrlFrame {
    // OpCode of the current Control Flow instruction
    public final OpCode opCode;
    // params or inputs
    public final int startValues;
    // returns or outputs
    public final int endValues;
    // the height of the stack before entering the current Control Flow instruction
    public final int height;

    // the program counter of a TRY_TABLE block
    // TODO: do we have a better way of doing this?
    public final int pc;

    public CtrlFrame(OpCode opCode, int startValues, int endValues, int height) {
        this(opCode, startValues, endValues, height, 0);
    }

    public CtrlFrame(OpCode opCode, int startValues, int endValues, int height, int pc) {
        this.opCode = opCode;
        this.startValues = startValues;
        this.endValues = endValues;
        this.height = height;
        this.pc = pc;
    }
}
