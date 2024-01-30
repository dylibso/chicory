package com.dylibso.chicory.wasm.types;

public class StartSection extends Section {
    private int startIndex;

    public StartSection() {
        super(SectionId.START);
    }

    public int startIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
}
