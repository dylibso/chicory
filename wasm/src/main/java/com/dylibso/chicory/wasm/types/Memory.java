package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * A memory definition.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-mem">Memories</a> for
 * reference.
 */
public final class Memory {
    private final MemoryLimits memoryLimits;

    /**
     * Construct a new instance.
     *
     * @param memoryLimits the memory limits (must not be {@code null})
     */
    public Memory(MemoryLimits memoryLimits) {
        this.memoryLimits = Objects.requireNonNull(memoryLimits, "memoryLimits");
    }

    /**
     * {@return the defined memory limits}
     */
    public MemoryLimits memoryLimits() {
        return memoryLimits;
    }
}
