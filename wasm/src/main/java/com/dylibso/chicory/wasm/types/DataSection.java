package com.dylibso.chicory.wasm.types;

public class DataSection extends Section {
    private DataSegment[] dataSegments;

    public DataSection(long id, DataSegment[] dataSegments) {
        super(id);
        this.dataSegments = dataSegments;
    }

    public DataSegment[] dataSegments() {
        return dataSegments;
    }
}
