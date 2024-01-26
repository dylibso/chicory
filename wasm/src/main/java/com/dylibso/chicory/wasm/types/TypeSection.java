package com.dylibso.chicory.wasm.types;

public class TypeSection extends Section {
    private FunctionType[] types;

    public TypeSection(long id, FunctionType[] types) {
        super(id);
        this.types = types;
    }

    public FunctionType[] types() {
        return types;
    }
}
