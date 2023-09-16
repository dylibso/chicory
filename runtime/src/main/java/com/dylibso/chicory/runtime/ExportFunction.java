package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;

/**
 * This represents an Exported function from the Wasm module.
 */
@FunctionalInterface
public interface ExportFunction {
    Value apply(Value... args) throws ChicoryException;
}
