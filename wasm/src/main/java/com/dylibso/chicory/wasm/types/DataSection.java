package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class DataSection extends Section {
    private final ArrayList<DataSegment> dataSegments;

    private DataSection(ArrayList<DataSegment> dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = dataSegments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(DataSection dataSection) {
        return new Builder(dataSection);
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

    public static final class Builder {
        private final ArrayList<DataSegment> dataSegments;

        private Builder() {
            this.dataSegments = new ArrayList<>();
        }

        private Builder(DataSection dataSection) {
            this.dataSegments = new ArrayList<>();
            this.dataSegments.addAll(dataSection.dataSegments);
        }

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
