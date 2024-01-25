package com.dylibso.chicory.wasm.types;

public class TypeSection extends Section {
    private FunctionType[] types;

    public TypeSection(FunctionType[] types) {
        super(SectionId.TYPE);
        this.types = types;
    }

    public FunctionType[] types() {
        return types;
    }
}
