package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExportSection extends Section {
    private final List<Export> exports;

    private ExportSection(List<Export> exports) {
        super(SectionId.EXPORT);
        this.exports = List.copyOf(exports);
    }

    public int exportCount() {
        return exports.size();
    }

    public Export getExport(int idx) {
        return exports.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Export> exports = new ArrayList<>();

        private Builder() {}

        /**
         * Add an export definition to this section.
         *
         * @param export the export to add to this section (must not be {@code null})
         * @return the Builder
         */
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
