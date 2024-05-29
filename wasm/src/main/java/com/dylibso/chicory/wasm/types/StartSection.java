package com.dylibso.chicory.wasm.types;

public class StartSection extends Section {
    private final long startIndex;

    public StartSection(long startIndex) {
        super(SectionId.START);
        this.startIndex = startIndex;
    }

    public long startIndex() {
        return startIndex;
    }
}
