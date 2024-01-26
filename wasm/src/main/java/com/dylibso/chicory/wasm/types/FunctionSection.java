package com.dylibso.chicory.wasm.types;

public class FunctionSection extends Section {
    private int[] typeIndices;

    public FunctionSection(long id, int[] typeIndices) {
        super(id);
        this.typeIndices = typeIndices;
    }

    public int[] typeIndices() {
        return typeIndices;
    }
}
