package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class ExportSection extends Section {
    private final ArrayList<Export> exports;

    public ExportSection(Export[] exports) {
        super(SectionId.EXPORT);
        this.exports = new ArrayList<>(List.of(exports));
    }

    public int exportCount() {
        return exports.size();
    }

    public Export getExport(int idx) {
        return exports.get(idx);
    }
}
