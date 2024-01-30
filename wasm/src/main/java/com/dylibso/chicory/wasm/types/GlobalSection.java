package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class GlobalSection extends Section {
    private final ArrayList<Global> globals;

    public GlobalSection(Global[] globals) {
        super(SectionId.GLOBAL);
        this.globals = new ArrayList<>(List.of(globals));
    }

    public Global[] globals() {
        return globals.toArray(Global[]::new);
    }

    public int globalCount() {
        return globals.size();
    }

    public Global getGlobal(int idx) {
        return globals.get(idx);
    }
}
