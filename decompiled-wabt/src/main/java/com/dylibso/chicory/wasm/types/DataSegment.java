package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

public abstract class DataSegment {
    private final byte[] data;

    DataSegment(byte[] data) {
        this.data = data.clone();
    }

    public byte[] data() {
        return data.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DataSegment)) {
            return false;
        }
        DataSegment that = (DataSegment) o;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
