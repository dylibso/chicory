package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * A memory definition.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-mem">Memories</a> for
 * reference.
 */
public final class Memory {
    private final MemoryLimits limits;

    /**
     * Construct a new instance.
     *
     * @param limits the memory limits (must not be {@code null})
     */
    public Memory(MemoryLimits limits) {
        this.limits = Objects.requireNonNull(limits, "memoryLimits");
    }

    /**
     * Returns the size limits (initial and optional maximum) of this memory.
     *
     * @return the defined {@link MemoryLimits}.
     */
    public MemoryLimits limits() {
        return limits;
    }

    /**
     * Compares this memory definition to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code Memory} with the same limits, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Memory)) {
            return false;
        }
        Memory memory = (Memory) o;
        return Objects.equals(limits, memory.limits);
    }

    /**
     * Computes the hash code for this memory definition.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(limits);
    }
}
