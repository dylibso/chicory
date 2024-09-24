package com.dylibso.chicory.runtime;

public class ExternalGlobal implements ExternalValue {
    private final GlobalInstance instance;
    private final String module;
    private final String name;

    public ExternalGlobal(String module, String name, GlobalInstance instance) {
        this.instance = instance;
        this.module = module;
        this.name = name;
    }

    public GlobalInstance instance() {
        return instance;
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
        return Type.GLOBAL;
    }
}
