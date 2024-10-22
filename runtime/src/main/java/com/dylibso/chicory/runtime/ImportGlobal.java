package com.dylibso.chicory.runtime;

public class ImportGlobal implements ImportValue {
    private final GlobalInstance instance;
    private final String module;
    private final String name;

    public ImportGlobal(String module, String name, GlobalInstance instance) {
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
    public ImportValue.Type type() {
        return Type.GLOBAL;
    }
}
