package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

/**
 * A HostFunction is an ExternalFunction that has been defined by the host.
 */
public class HostFunction extends ExternalFunction {
    public HostFunction(
            WasmFunctionHandle handle,
            String moduleName,
            String fieldName,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        super(handle, moduleName, fieldName, paramTypes, returnTypes);
    }
}
