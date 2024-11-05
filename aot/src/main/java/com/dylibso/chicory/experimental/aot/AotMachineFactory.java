package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;

/**
 * Machine factory implementation that AOT compiles function bodies.
 * All compilation is done in a single compile phase during instantiation
 * and is reused for all created machine instances.
 */
public final class AotMachineFactory implements Function<Instance, Machine> {

    private final WasmModule module;
    private final Function<Instance, Machine> factory;

    public AotMachineFactory(WasmModule module) {
        this.module = module;
        this.factory = AotCompiler.compileModule(module).machineFactory();
    }

    @Override
    public Machine apply(Instance instance) {
        if (instance.module() != module) {
            throw new IllegalArgumentException("Instance module does not match factory module");
        }
        return factory.apply(instance);
    }
}
