package com.dylibso.chicory.wasm.types;

public class StartSection extends Section {
    private long startIndex;

    public StartSection() {
        super(SectionId.START);
    }

    public long startIndex() {
        return startIndex;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }
}
