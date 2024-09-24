package com.dylibso.chicory.runtime;

public class ExternalMemory implements ExternalValue {
    private final String module;
    private final String name;
    private final Memory memory;

    public ExternalMemory(String module, String name, Memory memory) {
        this.module = module;
        this.name = name;
        this.memory = memory;
    }

    @Override
    public String module() {
        return module;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ExternalValue.Type type() {
        return Type.MEMORY;
    }

    public Memory memory() {
        return memory;
    }
}
