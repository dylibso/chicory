package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class MemorySection extends Section {
    private final ArrayList<Memory> memories;

    private MemorySection(ArrayList<Memory> memories) {
        super(SectionId.MEMORY);
        this.memories = memories;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MemorySection memorySection) {
        return new Builder(memorySection);
    }

    public int memoryCount() {
        return memories.size();
    }

    public Memory getMemory(int idx) {
        return memories.get(idx);
    }

    public static final class Builder {

        private final ArrayList<Memory> memories;

        private Builder() {
            this.memories = new ArrayList<>();
        }

        private Builder(MemorySection memorySection) {
            this.memories = new ArrayList<>();
            this.memories.addAll(memorySection.memories);
        }

        public Builder addMemory(Memory memory) {
            Objects.requireNonNull(memory, "memory");
            memories.add(memory);
            return this;
        }

        public MemorySection build() {
            return new MemorySection(memories);
        }
    }
}
