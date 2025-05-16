package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.ChicoryException;

/**
 * Machine implementation that compiles WASM function bodies to JVM byte code.
 * All compilation is done in a single compile phase during instantiation.
 */
public final class AotMachine implements Machine {

    private final Machine machine;

    public AotMachine(Instance instance) {
        this.machine = new MachineFactory(instance.module()).apply(instance);
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        return machine.call(funcId, args);
    }
}
