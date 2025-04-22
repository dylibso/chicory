package com.dylibso.chicory.wasm.types;

/**
 * Represents the Start Section of a WebAssembly module.
 * This section declares the start function, which is automatically executed
 * when the module is instantiated.
 */
public final class StartSection extends Section {
    private final long startIndex;

    /**
     * Constructs a new Start Section.
     *
     * @param startIndex the index of the start function in the function index space.
     */
    private StartSection(long startIndex) {
        super(SectionId.START);
        this.startIndex = startIndex;
    }

    /**
     * Returns the index of the start function declared in this section.
     *
     * @return the start function index.
     */
    public long startIndex() {
        return startIndex;
    }

    /**
     * Creates a new builder for constructing a {@link StartSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link StartSection} instances.
     */
    public static final class Builder {
        private long startIndex;

        private Builder() {}

        /**
         * Sets the index of the start function.
         *
         * @param startIndex the start function index.
         * @return this builder instance.
         */
        public Builder setStartIndex(long startIndex) {
            this.startIndex = startIndex;
            return this;
        }

        /**
         * Constructs the {@link StartSection} instance.
         *
         * @return the built {@link StartSection}.
         */
        public StartSection build() {
            return new StartSection(startIndex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof StartSection)) {
            return false;
        }
        StartSection that = (StartSection) o;
        return startIndex == that.startIndex;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(startIndex);
    }
}
