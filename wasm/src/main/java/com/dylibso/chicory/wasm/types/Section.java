package com.dylibso.chicory.wasm.types;

public class Section {
    private final int id;

    public Section(long id) {
        this.id = (int) id;
    }

    public int sectionId() {
        return id;
    }
}
