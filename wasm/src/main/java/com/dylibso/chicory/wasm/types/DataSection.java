package com.dylibso.chicory.wasm.types;

public class DataSection extends Section {
    private DataSegment[] dataSegments;

    public DataSection(long id, long size, DataSegment[] dataSegments) {
        super(id, size);
        this.dataSegments = dataSegments;
    }

    public DataSegment[] getDataSegments() {
        return dataSegments;
    }
}
