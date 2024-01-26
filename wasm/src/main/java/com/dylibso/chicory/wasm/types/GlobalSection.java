package com.dylibso.chicory.wasm.types;

public class GlobalSection extends Section {
    private Global[] globals;

    public GlobalSection(long id, Global[] globals) {
        super(id);
        this.globals = globals;
    }

    public Global[] globals() {
        return globals;
    }
}
