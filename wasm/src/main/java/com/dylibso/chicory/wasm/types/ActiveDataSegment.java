package com.dylibso.chicory.wasm.types;

import java.util.List;

public final class ActiveDataSegment extends DataSegment {
    private final long idx;
    private final List<Instruction> offsetInstructions;

    public ActiveDataSegment(long idx, List<Instruction> offsetInstructions, byte[] data) {
        super(data);
        this.idx = idx;
        this.offsetInstructions = List.copyOf(offsetInstructions);
    }

    public long index() {
        return idx;
    }

    public List<Instruction> offsetInstructions() {
        return offsetInstructions;
    }
}
