package com.dylibso.chicory.wasm.types;

public class ActiveDataSegment extends DataSegment {
    private long idx;
    private Instruction[] offset;

    public ActiveDataSegment(long idx, Instruction[] offset, byte[] data) {
        super(data);
        this.idx = idx;
        this.offset = offset;
    }

    public long getIdx() {
        return idx;
    }

    public Instruction[] getOffset() {
        return offset;
    }
}
