package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Memory Section in a WebAssembly module.
 * This section declares all linear memories defined within the module, excluding imported memories.
 */
public final class MemorySection extends Section {
    private final List<Memory> memories;

    private MemorySection(List<Memory> memories) {
        super(SectionId.MEMORY);
        this.memories = List.copyOf(memories);
    }

    /**
     * Returns the number of memories defined in this section.
     *
     * @return the count of memories.
     */
    public int memoryCount() {
        return memories.size();
    }

    /**
     * Returns the memory definition at the specified index.
     *
     * @param idx the index of the memory to retrieve.
     * @return the {@link Memory} definition at the given index.
     */
    public Memory getMemory(int idx) {
        return memories.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link MemorySection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link MemorySection} instances.
     */
    public static final class Builder {
        private final List<Memory> memories = new ArrayList<>();

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

        /**
         * Constructs the {@link MemorySection} instance from the added memories.
         *
         * @return the built {@link MemorySection}.
         */
        public MemorySection build() {
            return new MemorySection(memories);
        }
    }

    /**
     * Compares this memory section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code MemorySection} with the same memories, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof MemorySection)) {
            return false;
        }
        MemorySection that = (MemorySection) o;
        return Objects.equals(memories, that.memories);
    }

    /**
     * Computes the hash code for this memory section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(memories);
    }
}
