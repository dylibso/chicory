package com.dylibso.chicory.runtime;

/**
 * Runtime representation of a WasmGC struct instance.
 * Fields are stored as raw long values (same encoding as stack values).
 */
public final class WasmStruct implements WasmGcRef {
    private final int typeIdx;
    private final long[] fields;

    public WasmStruct(int typeIdx, long[] fields) {
        this.typeIdx = typeIdx;
        this.fields = fields;
    }

    @Override
    public int typeIdx() {
        return typeIdx;
    }

    public long field(int idx) {
        return fields[idx];
    }

    public void setField(int idx, long value) {
        fields[idx] = value;
    }

    public int fieldCount() {
        return fields.length;
    }
}
