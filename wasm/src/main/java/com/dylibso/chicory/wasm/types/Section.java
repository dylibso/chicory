package com.dylibso.chicory.wasm.types;

public class Section {
    private final int id;
    private final long size;

    public Section(long id, long size) {
        this.id = (int) id;
        this.size = size;
    }

    public int sectionId() {
        return id;
    }

    public long sectionSize() {
        return size;
    }
}
