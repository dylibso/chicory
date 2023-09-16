package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.Section;

public interface ParserListener {
    void onSection(Section section);
}
