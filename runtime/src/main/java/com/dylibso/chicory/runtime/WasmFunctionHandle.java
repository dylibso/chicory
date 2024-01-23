package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;

/**
 * Represents a Java function that can be called from Wasm.
 */
@FunctionalInterface
public interface WasmFunctionHandle {
    Value[] apply(Instance instance, Value... args);
}
