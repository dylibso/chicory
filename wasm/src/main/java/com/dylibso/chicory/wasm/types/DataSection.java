package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class DataSection extends Section {
    private final ArrayList<DataSegment> dataSegments;

    public DataSection(DataSegment[] dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = new ArrayList<>(List.of(dataSegments));
    }

    public DataSegment[] dataSegments() {
        return dataSegments.toArray(DataSegment[]::new);
    }

    public int dataSegmentCount() {
        return dataSegments.size();
    }

    public DataSegment getDataSegment(int idx) {
        return dataSegments.get(idx);
    }
}
