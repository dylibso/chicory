package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Data Section in a WebAssembly module.
 * This section declares initialized data that is copied into memory instances.
 */
public final class DataSection extends Section {
    private final List<DataSegment> dataSegments;

    private DataSection(List<DataSegment> dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = List.copyOf(dataSegments);
    }

    /**
     * Returns the data segments defined in this section.
     *
     * @return an array of {@link DataSegment} instances.
     */
    public DataSegment[] dataSegments() {
        return dataSegments.toArray(new DataSegment[0]);
    }

    /**
     * Returns the number of data segments defined in this section.
     *
     * @return the count of data segments.
     */
    public int dataSegmentCount() {
        return dataSegments.size();
    }

    /**
     * Returns the data segment at the specified index.
     *
     * @param idx the index of the data segment to retrieve.
     * @return the {@link DataSegment} at the given index.
     */
    public DataSegment getDataSegment(int idx) {
        return dataSegments.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link DataSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link DataSection} instances.
     */
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

        /**
         * Constructs the {@link DataSection} instance from the added data segments.
         *
         * @return the built {@link DataSection}.
         */
        public DataSection build() {
            return new DataSection(dataSegments);
        }
    }

    /**
     * Compares this data section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code DataSection} with the same data segments, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DataSection)) {
            return false;
        }
        DataSection that = (DataSection) o;
        return Objects.equals(dataSegments, that.dataSegments);
    }

    /**
     * Computes the hash code for this data section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(dataSegments);
    }
}
