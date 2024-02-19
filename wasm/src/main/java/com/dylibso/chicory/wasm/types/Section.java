package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;

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

    /**
     * Read this section from the given input.
     * Any bytes in this section not consumed will be skipped after this method returns.
     *
     * @param in the input (not {@code null})
     * @throws WasmIOException if an I/O error occurs while reading the section
     */
    public abstract void readFrom(WasmInputStream in) throws WasmIOException;
}
