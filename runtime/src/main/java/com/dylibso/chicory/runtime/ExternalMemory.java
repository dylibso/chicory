package com.dylibso.chicory.runtime;

public class ExternalMemory implements ExternalValue {
    private final String moduleName;
    private final String fieldName;
    private final Memory memory;

    public ExternalMemory(String moduleName, String fieldName, Memory memory) {
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
    public ExternalValue.Type type() {
        return Type.MEMORY;
    }

    public Memory memory() {
        return memory;
    }
}
