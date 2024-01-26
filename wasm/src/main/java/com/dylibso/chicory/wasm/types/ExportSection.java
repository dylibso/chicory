package com.dylibso.chicory.wasm.types;

public class ExportSection extends Section {
    private Export[] exports;

    public ExportSection(Export[] exports) {
        super(SectionId.EXPORT);
        this.exports = exports;
    }

    public Export[] exports() {
        return exports;
    }
}
