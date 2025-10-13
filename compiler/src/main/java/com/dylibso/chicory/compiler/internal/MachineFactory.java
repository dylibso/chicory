package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.CompiledModule;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;

/**
 * Machine factory implementation that AOT compiles function bodies.
 * All compilation is done in a single compile phase during instantiation
 * and is reused for all created machine instances.
 */
public final class MachineFactory implements Function<Instance, Machine>, CompiledModule {

    private final WasmModule module;
    private final Function<Instance, Machine> factory;

    public MachineFactory(WasmModule module) {
        this.module = module;
        var compiler =
                Compiler.builder(module)
                        .withClassCollectorFactory(ClassLoadingCollector::new)
                        .build();
        var result = compiler.compile();
        var collector = (ClassLoadingCollector) result.collector();
        this.factory = collector.machineFactory();
    }

    public MachineFactory(WasmModule module, Function<Instance, Machine> factory) {
        this.module = module;
        this.factory = factory;
    }

    @Override
    public Machine apply(Instance instance) {
        if (instance.module() != module) {
            throw new IllegalArgumentException("Instance module does not match factory module");
        }
        return factory.apply(instance);
    }

    @Override
    public WasmModule wasmModule() {
        return this.module;
    }

    @Override
    public Function<Instance, Machine> machineFactory() {
        return factory;
    }
}
