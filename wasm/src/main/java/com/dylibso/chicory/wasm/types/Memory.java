package com.dylibso.chicory.wasm.types;

public class Memory {
    private MemoryLimits memoryLimits;

    public Memory(MemoryLimits memoryLimits) {
        this.memoryLimits = memoryLimits;
    }

    public MemoryLimits getMemoryLimits() {
        return memoryLimits;
    }
}
