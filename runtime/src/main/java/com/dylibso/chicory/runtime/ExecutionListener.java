package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Instruction;

@FunctionalInterface
public interface ExecutionListener {
    void onExecution(Instruction instruction, long[] operands, MStack stack);
}
