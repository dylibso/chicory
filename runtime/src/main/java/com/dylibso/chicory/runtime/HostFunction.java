package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

/**
 * A HostFunction is an ExternalFunction that has been defined by the host.
 */
public class HostFunction extends ExternalFunction {
    public HostFunction(
            String moduleName,
            String fieldName,
            WasmFunctionHandle handle,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        super(moduleName, fieldName, handle, paramTypes, returnTypes);
    }
}
