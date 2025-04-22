package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

/**
 * Represents an active data segment in a WebAssembly module.
 * Active data segments are copied into a linear memory instance during instantiation.
 */
public final class ActiveDataSegment extends DataSegment {
    private final long idx;
    private final List<Instruction> offsetInstructions;

    /**
     * Constructs a new active data segment.
     *
     * @param idx the index of the linear memory to initialize.
     * @param offsetInstructions the list of instructions defining the offset where data should be written.
     * @param data the raw byte data to write into the memory.
     */
    public ActiveDataSegment(long idx, List<Instruction> offsetInstructions, byte[] data) {
        super(data);
        this.idx = idx;
        this.offsetInstructions = List.copyOf(offsetInstructions);
    }

    /**
     * Returns the index of the target linear memory.
     *
     * @return the memory index (currently always 0 in MVP).
     */
    public long index() {
        return idx;
    }

    /**
     * Returns the list of instructions that compute the offset in the linear memory
     * where the data should be written. This is typically a single `i32.const` instruction.
     *
     * @return the list of offset instructions.
     */
    public List<Instruction> offsetInstructions() {
        return offsetInstructions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ActiveDataSegment)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ActiveDataSegment that = (ActiveDataSegment) o;
        return idx == that.idx && Objects.equals(offsetInstructions, that.offsetInstructions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), idx, offsetInstructions);
    }
}
