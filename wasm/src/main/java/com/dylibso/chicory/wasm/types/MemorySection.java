package com.dylibso.chicory.wasm.types;

public class MemorySection extends Section {
    private Memory[] memories;

    public MemorySection(long id, Memory[] memories) {
        super(id);
        this.memories = memories;
    }

    public Memory[] memories() {
        return memories;
    }
}
