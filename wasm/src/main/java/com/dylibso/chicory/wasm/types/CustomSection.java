package com.dylibso.chicory.wasm.types;

/**
 * A custom section of some kind.
 */
public abstract class CustomSection extends Section {

    CustomSection() {
        super(SectionId.CUSTOM);
    }

    /**
     * Returns the name of this custom section.
     * Standard custom section names include "name", "linking", etc.
     *
     * @return the name string.
     */
    public abstract String name();
}
