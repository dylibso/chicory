package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class MemorySection extends Section {
    private final ArrayList<Memory> memories;

    /**
     * Construct a new, empty section instance.
     */
    public MemorySection() {
        this(1);
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of memories to reserve space for
     */
    public MemorySection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private MemorySection(ArrayList<Memory> memories) {
        super(SectionId.MEMORY);
        this.memories = memories;
    }

    public int memoryCount() {
        return memories.size();
    }

    public Memory getMemory(int idx) {
        return memories.get(idx);
    }

    /**
     * Add a memory definition to this section.
     *
     * @param memory the memory to add to this section (must not be {@code null})
     * @return the index of the newly-added memory
     */
    public int addMemory(Memory memory) {
        Objects.requireNonNull(memory, "memory");
        int idx = memories.size();
        memories.add(memory);
        return idx;
    }
}
