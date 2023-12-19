package com.dylibso.chicory.runtime;

public class HostMemory implements FromHost {
    private final String moduleName;
    private final String fieldName;
    private final Memory memory;

    public HostMemory(String moduleName, String fieldName, Memory memory) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
        this.memory = memory;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public FromHostType getType() {
        return FromHostType.MEMORY;
    }

    public Memory getMemory() {
        return memory;
    }
}
