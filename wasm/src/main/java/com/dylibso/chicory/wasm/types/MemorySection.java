package com.dylibso.chicory.wasm.types;

public class MemorySection extends Section {
    private Memory[] memories;

    public MemorySection(Memory[] memories) {
        super(SectionId.MEMORY);
        this.memories = memories;
    }

    public Memory[] memories() {
        return memories;
    }
}
