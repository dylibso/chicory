package com.dylibso.chicory.runtime;

/**
 * Runtime representation of a WasmGC array instance.
 * Elements are stored as raw long values (same encoding as stack values).
 * Packed types (i8, i16) are stored as full long slots for simplicity.
 */
public final class WasmArray implements WasmGcRef {
    private final int typeIdx;
    private final long[] elements;

    public WasmArray(int typeIdx, long[] elements) {
        this.typeIdx = typeIdx;
        this.elements = elements;
    }

    @Override
    public int typeIdx() {
        return typeIdx;
    }

    public long get(int idx) {
        return elements[idx];
    }

    public void set(int idx, long value) {
        elements[idx] = value;
    }

    public int length() {
        return elements.length;
    }

    public long[] elements() {
        return elements;
    }
}
