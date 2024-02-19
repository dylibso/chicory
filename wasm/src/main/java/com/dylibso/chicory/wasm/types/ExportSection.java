package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.Objects;

public final class ExportSection extends Section {
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

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var exportCount = in.u31();
        exports.ensureCapacity(exports.size() + exportCount);

        // Parse individual functions in the function section
        for (int i = 0; i < exportCount; i++) {
            var name = in.utf8();
            var exportType = ExternalType.byId(in.u8());
            var index = in.u31();
            addExport(new Export(name, index, exportType));
        }
    }
}
