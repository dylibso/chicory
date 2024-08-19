package com.dylibso.chicory.wasm.types;

public final class DataCountSection extends Section {
    private final int dataCount;

    private DataCountSection(int dataCount) {
        super(SectionId.DATA_COUNT);
        this.dataCount = dataCount;
    }

    public int dataCount() {
        return dataCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int dataCount;

        public Builder withDataCount(int dataCount) {
            this.dataCount = dataCount;
            return this;
        }

        public DataCountSection build() {
            return new DataCountSection(dataCount);
        }
    }
}
