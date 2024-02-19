package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
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

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        // do nothing
    }
}
