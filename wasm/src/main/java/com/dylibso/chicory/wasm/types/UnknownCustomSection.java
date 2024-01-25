package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * A custom section which is unknown to the parser.
 */
public final class UnknownCustomSection extends CustomSection {
    private final String name;
    private final byte[] bytes;

    /**
     * Construct a new instance.
     *
     * @param size the section size
     * @param name the name of the section (must not be {@code null})
     * @param bytes the section contents (must not be {@code null})
     */
    public UnknownCustomSection(final long size, final String name, final byte[] bytes) {
        super(size);
        this.name = Objects.requireNonNull(name, "name");
        this.bytes = bytes.clone();
    }

    public String name() {
        return name;
    }

    public byte[] bytes() {
        return bytes;
    }
}
