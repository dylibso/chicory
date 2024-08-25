package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DataSection extends Section {
    private final List<DataSegment> dataSegments;

    private DataSection(List<DataSegment> dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = List.copyOf(dataSegments);
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<DataSegment> dataSegments = new ArrayList<>();

        private Builder() {}

        /**
         * Add a data segment definition to this section.
         *
         * @param dataSegment the data segment to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addDataSegment(DataSegment dataSegment) {
            Objects.requireNonNull(dataSegment, "dataSegment");
            dataSegments.add(dataSegment);
            return this;
        }

        public DataSection build() {
            return new DataSection(dataSegments);
        }
    }
}
