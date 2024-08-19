package com.dylibso.chicory.wasm.types;

public abstract class DataSegment {
    private final byte[] data;

    DataSegment(byte[] data) {
        this.data = data.clone();
    }

    public byte[] data() {
        return data.clone();
    }
}
