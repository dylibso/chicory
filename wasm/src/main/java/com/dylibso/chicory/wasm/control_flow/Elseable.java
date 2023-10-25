package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;

public interface Elseable {

    public void onElse(Instruction instruction, int i);
}
