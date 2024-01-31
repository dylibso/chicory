package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class ExportSection extends Section {
    private final ArrayList<Export> exports;

    private ExportSection(ArrayList<Export> exports) {
        super(SectionId.EXPORT);
        this.exports = exports;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ExportSection exportSection) {
        return new Builder(exportSection);
    }

    public int exportCount() {
        return exports.size();
    }

    public Export getExport(int idx) {
        return exports.get(idx);
    }

    public static final class Builder {
        private final ArrayList<Export> exports;

        private Builder() {
            this.exports = new ArrayList<>();
        }

        private Builder(ExportSection exportSection) {
            this.exports = new ArrayList<>();
            this.exports.addAll(exportSection.exports);
        }

        public Builder addExport(Export export) {
            Objects.requireNonNull(export, "export");
            exports.add(export);
            return this;
        }

        public ExportSection build() {
            return new ExportSection(exports);
        }
    }
}
