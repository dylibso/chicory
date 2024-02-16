package com.dylibso.chicory.wasm.types;

/**
 * A section which is not known to the parser.
 */
public final class UnknownSection extends Section {
    /**
     * Construct a new instance.
     *
     * @param id the section ID
     */
    public UnknownSection(final int id) {
        super(id);
    }
}
