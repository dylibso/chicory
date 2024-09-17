package com.dylibso.chicory.runtime;

public class ExternalGlobal implements ExternalValue {
    private final GlobalInstance instance;
    private final String moduleName;
    private final String symbolName;

    public ExternalGlobal(String moduleName, String symbolName, GlobalInstance instance) {
        this.instance = instance;
        this.moduleName = moduleName;
        this.symbolName = symbolName;
    }

    public GlobalInstance instance() {
        return instance;
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public String symbolName() {
        return symbolName;
    }

    @Override
    public ExternalValue.Type type() {
        return Type.GLOBAL;
    }
}
