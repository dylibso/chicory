package com.dylibso.chicory.wasm.types;

/**
 * Base class for all WebAssembly module sections.
 * Each section has a numerical ID.
 */
public abstract class Section {
    private final int id;

    Section(long id) {
        this.id = (int) id;
    }

    /**
     * Returns the numerical identifier for this section type.
     * See {@link SectionId} for standard section IDs.
     * Custom sections use ID 0.
     *
     * @return the section ID.
     */
    public int sectionId() {
        return id;
    }
}
