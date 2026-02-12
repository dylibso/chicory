package com.dylibso.chicory.wasm.types;

/**
 * A custom section of some kind.
 */
public abstract class CustomSection extends Section {

    CustomSection() {
        super(SectionId.CUSTOM);
    }

    public abstract String name();
}
