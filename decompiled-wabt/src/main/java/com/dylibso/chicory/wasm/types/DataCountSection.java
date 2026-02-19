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

    @Override
    public int hashCode() {
        return Integer.hashCode(dataCount);
    }
}
