package com.dylibso.chicory.wasm.types;

public class GlobalSection extends Section {
    private Global[] globals;

    public GlobalSection(Global[] globals) {
        super(SectionId.GLOBAL);
        this.globals = globals;
    }

    public Global[] globals() {
        return globals;
    }
}
