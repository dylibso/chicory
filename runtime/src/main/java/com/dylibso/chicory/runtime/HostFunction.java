package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.List;

/**
 * A HostFunction is an ExternalFunction that has been defined by the host.
 */
public class HostFunction extends ImportFunction {
    @Deprecated(since = "23/05/2025", forRemoval = true)
    public HostFunction(
            String moduleName,
            String symbolName,
            List paramTypes,
            List returnTypes,
            WasmFunctionHandle handle) {
        super(
                moduleName,
                symbolName,
                FunctionType.of(convert(paramTypes), convert(returnTypes)),
                handle);
    }

    public HostFunction(
            String moduleName, String symbolName, FunctionType type, WasmFunctionHandle handle) {
        super(moduleName, symbolName, type, handle);
    }
}
