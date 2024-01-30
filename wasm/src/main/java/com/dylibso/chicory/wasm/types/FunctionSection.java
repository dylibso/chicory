package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public class FunctionSection extends Section {
    private int[] typeIndices;
    private int count;

    public FunctionSection(int[] typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = typeIndices.clone();
        count = typeIndices.length;
    }

    public int functionCount() {
        return count;
    }

    public int getFunctionType(int idx) {
        Objects.checkIndex(idx, count);
        return typeIndices[idx];
    }

    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        return typeSection.getType(getFunctionType(idx));
    }
}
