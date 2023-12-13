package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;

public class HostGlobal implements FromHost {
    private Value value;
    private final MutabilityType type;
    private final String moduleName;
    private final String fieldName;

    public HostGlobal(String moduleName, String fieldName, Value value) {
        this(moduleName, fieldName, value, MutabilityType.Const);
    }

    public HostGlobal(String moduleName, String fieldName, Value value, MutabilityType type) {
        this.value = value;
        this.type = type;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public MutabilityType getMutabilityType() {
        return type;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public FromHostType getType() {
        return FromHostType.GLOBAL;
    }
}
