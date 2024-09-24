package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

/**
 * This represents an Exported function from the Wasm module.
 */
@FunctionalInterface
public interface ExportFunction {
    long[] apply(long... args) throws ChicoryException;
}
