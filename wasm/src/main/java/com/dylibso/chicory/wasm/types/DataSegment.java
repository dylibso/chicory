package com.dylibso.chicory.wasm.types;

public class DataSegment {
    private byte[] data;

    public DataSegment(byte[] data) {
        this.data = data;
    }

    public byte[] data() {
        return data;
    }
}
