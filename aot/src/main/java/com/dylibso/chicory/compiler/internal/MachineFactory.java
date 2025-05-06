package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;

/**
 * Machine factory implementation that AOT compiles function bodies.
 * All compilation is done in a single compile phase during instantiation
 * and is reused for all created machine instances.
 */
public final class MachineFactory implements Function<Instance, Machine> {

    private final WasmModule module;
    private final Function<Instance, Machine> factory;

    public MachineFactory(WasmModule module) {
        this.module = module;
        var compiler = Compiler.builder(module).build();
        var result = compiler.compile();
        this.factory = result.machineFactory();
    }

    @Override
    public Machine apply(Instance instance) {
        if (instance.module() != module) {
            throw new IllegalArgumentException("Instance module does not match factory module");
        }
        return factory.apply(instance);
    }
}
