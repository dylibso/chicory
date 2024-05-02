package com.dylibso.chicory.aot;

import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public class AotMachine implements Machine {

    protected final Module module;

    public AotMachine(Module module) {
        this.module = module;
    }

    @Override
    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        return new Value[0];
    }

    @Override
    public List<StackFrame> getStackTrace() {
        return List.of();
    }
}
