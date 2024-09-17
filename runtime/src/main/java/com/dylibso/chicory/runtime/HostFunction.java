package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

/**
 * A HostFunction is an ExternalFunction that has been defined by the host.
 */
public class HostFunction extends ExternalFunction {
    /**
     * @deprecated use {@link HostFunction#HostFunction(String, String, WasmFunctionHandle,
     * List, List)}
     */
    @Deprecated
    public HostFunction(
            WasmFunctionHandle handle,
            String moduleName,
            String fieldName,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        this(moduleName, fieldName, handle, paramTypes, returnTypes);
    }

    public HostFunction(
            String moduleName,
            String fieldName,
            WasmFunctionHandle handle,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        super(moduleName, fieldName, handle, paramTypes, returnTypes);
    }
}
