package com.dylibso.chicory.wasm.types;

/**
 * Represents a section of a WebAssembly module whose specific type is unknown or unhandled by the parser.
 * It stores the section ID and its raw byte content.
 */
public class RawSection extends Section {
    private final byte[] contents;

    /**
     * Constructs a new RawSection.
     *
     * @param id the numerical ID of the section.
     * @param contents the raw byte content of the section (will be cloned).
     */
    public RawSection(long id, byte[] contents) {
        super(id);
        this.contents = contents.clone();
    }

    /**
     * Returns a copy of the raw byte content of this section.
     * A clone is returned to prevent modification of the internal array.
     *
     * @return a copy of the raw byte data.
     */
    public byte[] contents() {
        return contents.clone();
    }
}
