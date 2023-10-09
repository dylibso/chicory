package com.dylibso.chicory.wasm.types;

public class ExportSection extends Section {
    private Export[] exports;

    public ExportSection(long id, long size, Export[] exports) {
        super(id, size);
        this.exports = exports;
    }

    public Export[] getExports() {
        return exports;
    }
}
