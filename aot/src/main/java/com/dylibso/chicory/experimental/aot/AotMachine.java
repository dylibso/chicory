package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.ChicoryException;

/**
 * Machine implementation that compiles WASM function bodies to JVM byte code.
 * All compilation is done in a single compile phase during instantiation.
 * <p>
 * This class is deprecated and will be removed in a future version. Please use
 * the {@link com.dylibso.chicory.compiler.MachineFactoryCompiler} instead.
 */
@Deprecated(since = "1.4.0", forRemoval = true)
public final class AotMachine implements Machine {

    private final Machine machine;

    /**
     * Creates a new AOT machine instance.
     * <p>
     * Please use the {@link com.dylibso.chicory.compiler.MachineFactoryCompiler#compile(Instance)} method instead.
     *
     * @param instance the instance to use for the machine
     */
    @Deprecated(since = "1.4.0", forRemoval = true)
    public AotMachine(Instance instance) {
        this.machine = new MachineFactory(instance.module()).apply(instance);
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        return machine.call(funcId, args);
    }
}
