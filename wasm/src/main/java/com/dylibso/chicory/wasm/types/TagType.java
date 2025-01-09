package com.dylibso.chicory.wasm.types;

public class TagType {
    private final byte attribute;
    private final int typeIdx;

    public TagType(byte attribute, int typeIdx) {
        this.attribute = attribute;
        this.typeIdx = typeIdx;
    }

    public byte attribute() {
        return attribute;
    }

    public int typeIdx() {
        return typeIdx;
    }
}
