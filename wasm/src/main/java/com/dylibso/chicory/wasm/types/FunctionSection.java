package com.dylibso.chicory.wasm.types;

public class FunctionSection extends Section {
    private int[] typeIndices;

    public FunctionSection(int[] typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = typeIndices;
    }

    public int[] typeIndices() {
        return typeIndices;
    }
}
