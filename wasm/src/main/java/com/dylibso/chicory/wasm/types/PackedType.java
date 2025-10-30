package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;

public enum PackedType {
    I8(0x78),
    I16(0x77);

    private final int id;

    public static PackedType fromId(int id) {
        switch (id) {
            case 0x78:
                return I8;
            case 0x77:
                return I16;
            default:
                throw new MalformedException("invalid packed type id: " + id);
        }
    }

    PackedType(int id) {
        this.id = id;
    }

    public int ID() {
        return id;
    }
}
