package com.dylibso.chicory.runtime;

/**
 * Represents a Java function that can be called from Wasm.
 */
@FunctionalInterface
public interface WasmFunctionHandle {
    long[] apply(Instance instance, long... args);
}
