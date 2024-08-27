package com.dylibso.chicory.runtime;

public class HostGlobal implements FromHost {
    private final GlobalInstance instance;
    private final String moduleName;
    private final String fieldName;

    public HostGlobal(String moduleName, String fieldName, GlobalInstance instance) {
        this.instance = instance;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public GlobalInstance instance() {
        return instance;
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
        return FromHostType.GLOBAL;
    }
}
