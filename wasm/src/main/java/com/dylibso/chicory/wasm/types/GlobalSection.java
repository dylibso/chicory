package com.dylibso.chicory.wasm.types;

public class GlobalSection extends Section {
    private Global[] globals;

    public GlobalSection(long id, long size, Global[] globals) {
        super(id, size);
        this.globals = globals;
    }

    public Global[] getGlobals() {
        return globals;
    }
}
