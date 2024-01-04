package com.dylibso.chicory.wasm.types;

public class ActiveDataSegment extends DataSegment {
    private final long idx;
    private final Instruction[] offset;

    public ActiveDataSegment(Instruction[] offset, byte[] data) {
        this(0, offset, data);
    }

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
