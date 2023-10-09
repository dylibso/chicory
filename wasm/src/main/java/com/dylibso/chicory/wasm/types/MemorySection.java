package com.dylibso.chicory.wasm.types;

public class MemorySection extends Section {
    private Memory[] memories;

    public MemorySection(long id, long size, Memory[] memories) {
        super(id, size);
        this.memories = memories;
    }

    public Memory[] getMemories() {
        return memories;
    }
}
