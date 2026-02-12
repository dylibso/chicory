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
     * @return the defined memory limits
     */
    public MemoryLimits limits() {
        return limits;
    }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(limits);
    }
}
