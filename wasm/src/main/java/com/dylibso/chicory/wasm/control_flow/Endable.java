package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;

public interface Endable {

    public void onEnd(Instruction instruction, int i);
}
