package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class ExportSection extends Section {
    private final ArrayList<Export> exports;

    /**
     * Construct a new, empty section instance.
     */
    public ExportSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of exports to reserve space for
     */
    public ExportSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private ExportSection(ArrayList<Export> exports) {
        super(SectionId.EXPORT);
        this.exports = exports;
    }

    public int exportCount() {
        return exports.size();
    }

    public Export getExport(int idx) {
        return exports.get(idx);
    }

    /**
     * Add an export definition to this section.
     *
     * @param export the export to add to this section (must not be {@code null})
     * @return the index of the newly-added export
     */
    public int addExport(Export export) {
        Objects.requireNonNull(export, "export");
        int idx = exports.size();
        exports.add(export);
        return idx;
    }
}
