package com.dylibso.chicory.wasm.types;

public class ExportSection extends Section {
    private Export[] exports;

    public ExportSection(long id, Export[] exports) {
        super(id);
        this.exports = exports;
    }

    public Export[] exports() {
        return exports;
    }
}
