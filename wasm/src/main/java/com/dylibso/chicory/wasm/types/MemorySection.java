package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class MemorySection extends Section {
    private final ArrayList<Memory> memories;

    public MemorySection(Memory[] memories) {
        super(SectionId.MEMORY);
        this.memories = new ArrayList<>(List.of(memories));
    }

    public int memoryCount() {
        return memories.size();
    }

    public Memory getMemory(int idx) {
        return memories.get(idx);
    }
}
