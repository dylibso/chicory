package com.dylibso.chicory.wasm.types;

/**
 * A custom section of some kind.
 */
public abstract class CustomSection extends Section {

    protected CustomSection() {
        super(SectionId.CUSTOM);
    }

    public abstract String name();

    public interface Builder {
        Builder withBytes(byte[] bytes);

        CustomSection build();
    }
}
