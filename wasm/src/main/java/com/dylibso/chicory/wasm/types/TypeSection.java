package com.dylibso.chicory.wasm.types;

public class TypeSection extends Section {
    private Type[] types;

    public TypeSection(long id, long size, Type[] types) {
        super(id, size);
        this.types = types;
    }

    public Type[] getTypes() {
        return types;
    }
}
