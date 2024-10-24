package com.dylibso.chicory.aot;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.Map;

/**
 * Machine implementation that AOT compiles function bodies.
 * All compilation is done in a single compile phase during instantiation.
 */
public final class AotMachine implements Machine {

    private final Machine machine;

    public AotMachine(Instance instance) {
        this.machine = new AotMachineFactory(instance.module()).apply(instance);
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        return machine.call(funcId, args);
    }

    // TODO: implement SIMD support
    @Override
    public Map<OpCode, OpImpl> additionalOpCodes() {
        return Map.of();
    }
}
