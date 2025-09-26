package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.ChicoryException;

// TODO: maybe move all those new types to a separate package
public enum PackedType {
    I8(0x78),
    I16(0x79);

    private final int value;

    PackedType(int value) {
        this.value = value;
    }

    public static PackedType fromValue(int value) {
        switch (value) {
            case 0x78:
                return I8;
            case 0x79:
                return I16;
            default:
                throw new ChicoryException("Unrecognized packed type");
        }
    }

    public int value() {
        return value;
    }
}
