package com.dylibso.chicory.wasm.types;

/**
 * A custom section of some kind.
 */
public abstract class CustomSection extends Section {

    protected CustomSection(long size) {
        super(SectionId.CUSTOM, size);
    }

    public abstract String name();
}
