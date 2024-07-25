package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.OpCode;

public class CtrlFrame {
    // OpCode of the current Control Flow instruction
    public final OpCode opCode;
    // params or inputs
    public final int startValues;
    // returns or outputs
    public final int endValues;
    // the height of the stack before entering the current Control Flow instruction
    public final int height;

    public CtrlFrame(OpCode opCode, int startValues, int endValues, int height) {
        this.opCode = opCode;
        this.startValues = startValues;
        this.endValues = endValues;
        this.height = height;
    }
}
