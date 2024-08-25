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
        return FromHostType.MEMORY;
    }

    public Memory memory() {
        return memory;
    }
}
