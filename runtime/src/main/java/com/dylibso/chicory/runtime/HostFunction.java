package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class HostFunction implements FromHost {
    private final WasmFunctionHandle handle;
    private final String moduleName;
    private final String fieldName;
    private final List<ValueType> paramTypes;
    private final List<ValueType> returnTypes;
    private boolean override;

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

    public WasmFunctionHandle getHandle() {
        return handle;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public FromHostType getType() {
        return FromHostType.FUNCTION;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    @Override
    public boolean override() {
        return override;
    }

    public List<ValueType> getParamTypes() {
        return paramTypes;
    }

    public List<ValueType> getReturnTypes() {
        return returnTypes;
    }
}
