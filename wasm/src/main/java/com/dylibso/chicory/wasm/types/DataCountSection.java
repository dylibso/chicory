package com.dylibso.chicory.wasm.types;

public class DataCountSection extends Section {

    private final int dataCount;

    public DataCountSection(int dataCount) {
        super(SectionId.DATA_COUNT);
        this.dataCount = dataCount;
    }

    public int dataCount() {
        return dataCount;
    }
}
