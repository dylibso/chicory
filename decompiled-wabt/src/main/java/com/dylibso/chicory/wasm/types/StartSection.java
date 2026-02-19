package com.dylibso.chicory.wasm.types;

public final class StartSection extends Section {
    private final long startIndex;

    private StartSection(long startIndex) {
        super(SectionId.START);
        this.startIndex = startIndex;
    }

    public long startIndex() {
        return startIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long startIndex;

        private Builder() {}

        public Builder setStartIndex(long startIndex) {
            this.startIndex = startIndex;
            return this;
        }

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
