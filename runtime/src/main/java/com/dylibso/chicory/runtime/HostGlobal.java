package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;

public class HostGlobal implements FromHost {
    private GlobalInstance instance;
    private final MutabilityType type;
    private final String moduleName;
    private final String fieldName;

    public HostGlobal(String moduleName, String fieldName, GlobalInstance instance) {
        this(moduleName, fieldName, instance, MutabilityType.Const);
    }

    public HostGlobal(
            String moduleName, String fieldName, GlobalInstance instance, MutabilityType type) {
        this.instance = instance;
        this.type = type;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public GlobalInstance instance() {
        return instance;
    }

    public void setInstance(GlobalInstance instance) {
        this.instance = instance;
    }

    public MutabilityType mutabilityType() {
        return type;
    }

    public String moduleName() {
        return moduleName;
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public FromHostType type() {
        return FromHostType.GLOBAL;
    }
}
