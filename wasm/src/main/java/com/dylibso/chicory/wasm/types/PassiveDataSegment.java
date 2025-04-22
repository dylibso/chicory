package com.dylibso.chicory.wasm.types;

/**
 * Represents a passive data segment in a WebAssembly module.
 * Passive data segments are not automatically copied into memory during instantiation
 * but can be copied using the `memory.init` instruction.
 */
public final class PassiveDataSegment extends DataSegment {
    /** A shared instance representing an empty passive data segment. */
    public static final PassiveDataSegment EMPTY = new PassiveDataSegment(new byte[] {});

    /**
     * Constructs a new passive data segment.
     *
     * @param data the raw byte data contained in the segment.
     */
    public PassiveDataSegment(byte[] data) {
        super(data);
    }
}
