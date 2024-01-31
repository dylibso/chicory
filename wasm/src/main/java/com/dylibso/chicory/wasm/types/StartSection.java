package com.dylibso.chicory.wasm.types;

public class StartSection extends Section {
    private long startIndex;

    private StartSection(long startIndex) {
        super(SectionId.START);
        this.startIndex = startIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long startIndex() {
        return startIndex;
    }

    public static final class Builder {
        private long startIndex;

        private Builder() {}

        public Builder withStartIndex(long startIndex) {
            this.startIndex = startIndex;
            return this;
        }

        public StartSection build() {
            return new StartSection(startIndex);
        }
    }
}
