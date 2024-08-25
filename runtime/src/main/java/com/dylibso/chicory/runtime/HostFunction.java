package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class HostFunction implements FromHost {
    private final WasmFunctionHandle handle;
    private final String moduleName;
    private final String fieldName;
    private final List<ValueType> paramTypes;
    private final List<ValueType> returnTypes;

    public HostFunction(
            WasmFunctionHandle handle,
            String moduleName,
            String fieldName,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        this.handle = handle;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
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
    public String fieldName() {
        return fieldName;
    }

    @Override
    public FromHostType type() {
        return FromHostType.FUNCTION;
    }

    public List<ValueType> paramTypes() {
        return paramTypes;
    }

    public List<ValueType> returnTypes() {
        return returnTypes;
    }
}
