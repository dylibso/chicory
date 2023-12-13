package com.dylibso.chicory.runtime;

// TODO: implement me
public class HostTable implements FromHost {
    private final String moduleName;
    private final String fieldName;

    public HostTable(String moduleName, String fieldName) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public FromHostType getType() {
        return FromHostType.TABLE;
    }
}
