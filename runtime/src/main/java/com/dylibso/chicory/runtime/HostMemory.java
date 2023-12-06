package com.dylibso.chicory.runtime;

// TODO: finish me
public class HostMemory {
    private final String moduleName;
    private final String fieldName;

    public HostMemory(String moduleName, String fieldName) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
