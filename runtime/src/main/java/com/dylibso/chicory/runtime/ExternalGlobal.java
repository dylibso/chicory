package com.dylibso.chicory.runtime;

public class ExternalGlobal implements ExternalValue {
    private final GlobalInstance instance;
    private final String moduleName;
    private final String fieldName;

    public ExternalGlobal(String moduleName, String fieldName, GlobalInstance instance) {
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
    public Type type() {
        return Type.GLOBAL;
    }
}
