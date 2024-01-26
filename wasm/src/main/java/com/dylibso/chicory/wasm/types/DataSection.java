package com.dylibso.chicory.wasm.types;

public class DataSection extends Section {
    private DataSegment[] dataSegments;

    public DataSection(DataSegment[] dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = dataSegments;
    }

    public DataSegment[] dataSegments() {
        return dataSegments;
    }
}
