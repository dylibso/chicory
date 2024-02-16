package com.dylibso.chicory.wasm.types;

/**
 * A section of a WASM file.
 */
public abstract class Section {
    private final int id;

    /**
     * Construct a new instance.
     *
     * @param id the section identifier
     */
    protected Section(int id) {
        this.id = id;
    }

    /**
     * {@return the section identifier}
     */
    public int sectionId() {
        return id;
    }
}
