package com.dylibso.chicory.wasm.types;

import java.util.List;

public class RecursiveType implements RecType {
    private final List<SubType> subTypes;
    private final SubType subType;

    // TODO: move to a Builder
    public RecursiveType(List<SubType> subTypes, SubType subType) {
        this.subTypes = subTypes;
        this.subType = subType;
    }

    public List<SubType> getSubTypes() {
        return subTypes;
    }

    public SubType getSubType() {
        return subType;
    }
}
