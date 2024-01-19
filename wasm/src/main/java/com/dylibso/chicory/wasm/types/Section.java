package com.dylibso.chicory.wasm.types;

public class Section {
    private final int id;
    private final long size;

    public Section(long id, long size) {
        this.id = (int) id;
        this.size = size;
    }

    public int getSectionId() {
        return id;
    }

    public long getSectionSize() {
        return size;
    }
}
