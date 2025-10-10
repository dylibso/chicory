package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

public class SubType {
    private final int[] typeIdx;
    private final CompType compType;
    private final boolean isFinal;

    // TODO: builders
    public SubType(int[] typeIdx, CompType compType, boolean isFinal) {
        this.typeIdx = typeIdx.clone();
        this.compType = compType;
        this.isFinal = isFinal;
    }

    public int[] typeIdx() {
        return typeIdx;
    }

    public CompType compType() {
        return compType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubType subType = (SubType) o;
        return Objects.deepEquals(typeIdx, subType.typeIdx)
                && Objects.equals(compType, subType.compType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(typeIdx), compType);
    }
}
