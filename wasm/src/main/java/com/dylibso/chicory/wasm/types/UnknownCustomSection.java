package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * A custom section which is unknown to the parser.
 */
public final class UnknownCustomSection extends CustomSection {
    private final String name;

    /**
     * Construct a new instance.
     *
     * @param name the name of the section (must not be {@code null})
     */
    public UnknownCustomSection(final String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }
}
