package com.dylibso.chicory.wasm.types;

public class TypeSection extends Section {
    private FunctionType[] types;

    public TypeSection(long id, long size, FunctionType[] types) {
       super(id, size);
       this.types = types;
    }

    public FunctionType[] getTypes() {
        return types;
    }
}
