package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.Section;

@FunctionalInterface
public interface ParserListener {

    void onSection(Section section);
}
