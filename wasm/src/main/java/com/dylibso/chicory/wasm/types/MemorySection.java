package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MemorySection extends Section {
    private final List<Memory> memories;

    private MemorySection(List<Memory> memories) {
        super(SectionId.MEMORY);
        this.memories = List.copyOf(memories);
    }

    public int memoryCount() {
        return memories.size();
    }

    public Memory getMemory(int idx) {
        return memories.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Memory> memories = new ArrayList<>();

        private Builder() {}

        /**
         * Add a memory definition to this section.
         *
         * @param memory the memory to add to this section (must not be {@code null})
         * @return the Builder
         */
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
