package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a data segment, which initializes a range of memory.
 * Data segments can be active (initialize memory on instantiation) or passive
 * (initialized programmatically via `memory.init`).
 * This is the base class for {@link ActiveDataSegment} and {@link PassiveDataSegment}.
 */
public abstract class DataSegment {
    private final byte[] data;

    DataSegment(byte[] data) {
        this.data = data.clone();
    }

    /**
     * Returns a copy of the raw byte data contained in this segment.
     * A clone is returned to prevent modification of the internal array.
     *
     * @return a copy of the initialization data.
     */
    public byte[] data() {
        return data.clone();
    }

    /**
     * Compares this data segment to another object for equality.
     * Comparison is based on the raw byte data content.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code DataSegment} with the same byte data, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this data segment.
     * The hash code is based on the raw byte data content.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
