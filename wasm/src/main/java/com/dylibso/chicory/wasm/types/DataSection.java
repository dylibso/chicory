package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public final class DataSection extends Section {
    private final ArrayList<DataSegment> dataSegments;

    /**
     * Construct a new, empty section instance.
     */
    public DataSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of data segments to reserve space for
     */
    public DataSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private DataSection(ArrayList<DataSegment> dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = dataSegments;
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

    /**
     * Add a data segment definition to this section.
     *
     * @param dataSegment the data segment to add to this section (must not be {@code null})
     * @return the index of the newly-added data segment
     */
    public int addDataSegment(DataSegment dataSegment) {
        Objects.requireNonNull(dataSegment, "dataSegment");
        int idx = dataSegments.size();
        dataSegments.add(dataSegment);
        return idx;
    }
}
