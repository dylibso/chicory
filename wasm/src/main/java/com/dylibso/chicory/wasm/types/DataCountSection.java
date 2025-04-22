package com.dylibso.chicory.wasm.types;

/**
 * Represents the Data Count Section in a WebAssembly module.
 * This section is optional and declares the number of data segments present in the Data Section.
 * It is required if the module uses the `memory.init` or `data.drop` instructions.
 */
public final class DataCountSection extends Section {
    private final int dataCount;

    private DataCountSection(int dataCount) {
        super(SectionId.DATA_COUNT);
        this.dataCount = dataCount;
    }

    /**
     * Returns the number of data segments declared in this section.
     *
     * @return the data segment count.
     */
    public int dataCount() {
        return dataCount;
    }

    /**
     * Creates a new builder for constructing a {@link DataCountSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link DataCountSection} instances.
     */
    public static final class Builder {
        private int dataCount;

        private Builder() {}

        /**
         * Sets the data count for the section.
         *
         * @param dataCount the number of data segments.
         * @return this builder instance.
         */
        public Builder withDataCount(int dataCount) {
            this.dataCount = dataCount;
            return this;
        }

        /**
         * Constructs the {@link DataCountSection} instance.
         *
         * @return the built {@link DataCountSection}.
         */
        public DataCountSection build() {
            return new DataCountSection(dataCount);
        }
    }

    /**
     * Compares this data count section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code DataCountSection} with the same data count, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DataCountSection)) {
            return false;
        }
        DataCountSection that = (DataCountSection) o;
        return dataCount == that.dataCount;
    }

    /**
     * Computes the hash code for this data count section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(dataCount);
    }
}
