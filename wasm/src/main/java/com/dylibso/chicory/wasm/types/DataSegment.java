package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public abstract class DataSegment {
    private final byte[] data;

    public DataSegment(byte[] data) {
        this.data = Objects.requireNonNull(data);
    }

    public byte[] data() {
        return data;
    }
}
