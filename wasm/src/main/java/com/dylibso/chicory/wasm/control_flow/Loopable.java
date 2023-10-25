package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;

public interface Loopable {

    public void onLoop(Instruction instruction, int i);
}
