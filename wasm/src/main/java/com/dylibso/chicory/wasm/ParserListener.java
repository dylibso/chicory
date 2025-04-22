package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.Section;

/**
 * A listener interface for receiving notifications when the {@link Parser}
 * successfully parses a section of a WebAssembly module.
 * This can be used for processing sections as they are found, rather than
 * waiting for the entire module to be parsed.
 */
@FunctionalInterface
public interface ParserListener {

    /**
     * Called when a Wasm section has been parsed.
     *
     * @param section The parsed {@link Section} object. This could be a specific
     *                section type (e.g., {@link com.dylibso.chicory.wasm.types.CodeSection}) or
     *                a {@link com.dylibso.chicory.wasm.types.RawSection} if parsing without decoding.
     */
    void onSection(Section section);
}
