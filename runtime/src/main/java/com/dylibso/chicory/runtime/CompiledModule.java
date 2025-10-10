package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;

/**
 * This interface is implemented by build time compiled wasm modules.
 */
public interface CompiledModule {
    WasmModule wasmModule();

    Function<Instance, Machine> machineFactory();
}
