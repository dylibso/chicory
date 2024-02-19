package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;

/**
 * A section which is not known to the parser.
 */
public final class UnknownSection extends Section {
    /**
     * Construct a new instance.
     *
     * @param id the section ID
     */
    public UnknownSection(final int id) {
        super(id);
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        // do nothing
    }
}
