package com.dylibso.chicory.wasm.types;

public class DataSegment {
    private long idx;
    private Instruction[] offset;
    private byte[] data;

    public DataSegment(long idx, Instruction[] offset, byte[] data) {
        this.idx = idx;
        this.offset = offset;
        this.data = data;
    }

    public long getIdx() {
        return idx;
    }

    public Instruction[] getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }
}
