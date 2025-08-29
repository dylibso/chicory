package com.dylibso.chicory.wasm.types;

import java.util.List;

public class SubType implements RecType {
    private final List<CompType> compTypes;
    private final CompType compType;

    // TODO: use a builder instead
    public SubType(List<CompType> compTypes, CompType compType) {
        this.compTypes = compTypes;
        this.compType = compType;
    }
}
