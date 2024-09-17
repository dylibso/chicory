package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class ExternalFunction implements ExternalValue {
    private final WasmFunctionHandle handle;
    private final String moduleName;
    private final String symbolName;
    private final List<ValueType> paramTypes;
    private final List<ValueType> returnTypes;

    public ExternalFunction(
            String moduleName,
            String symbolName,
            WasmFunctionHandle handle,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        this.handle = handle;
        this.moduleName = moduleName;
        this.symbolName = symbolName;
        this.paramTypes = paramTypes;
        this.returnTypes = returnTypes;
    }

    public WasmFunctionHandle handle() {
        return handle;
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public String symbolName() {
        return symbolName;
    }

    @Override
    public Type type() {
        return Type.FUNCTION;
    }

    public List<ValueType> paramTypes() {
        return paramTypes;
    }

    public List<ValueType> returnTypes() {
        return returnTypes;
    }
}
