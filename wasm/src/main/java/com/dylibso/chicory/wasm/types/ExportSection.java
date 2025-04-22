package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Export Section in a WebAssembly module.
 * This section declares all items (functions, tables, memories, globals, tags) that are exported from the module.
 */
public final class ExportSection extends Section {
    private final List<Export> exports;

    private ExportSection(List<Export> exports) {
        super(SectionId.EXPORT);
        this.exports = List.copyOf(exports);
    }

    /**
     * Returns the number of exports defined in this section.
     *
     * @return the count of exports.
     */
    public int exportCount() {
        return exports.size();
    }

    /**
     * Returns the export definition at the specified index.
     *
     * @param idx the index of the export to retrieve.
     * @return the {@link Export} definition at the given index.
     */
    public Export getExport(int idx) {
        return exports.get(idx);
    }

    /**
     * Creates a new builder for constructing an {@link ExportSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ExportSection} instances.
     */
    public static final class Builder {
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

        /**
         * Constructs the {@link ExportSection} instance from the added exports.
         *
         * @return the built {@link ExportSection}.
         */
        public ExportSection build() {
            return new ExportSection(exports);
        }
    }

    /**
     * Compares this export section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is an {@code ExportSection} with the same exports, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ExportSection)) {
            return false;
        }
        ExportSection that = (ExportSection) o;
        return Objects.equals(exports, that.exports);
    }

    /**
     * Computes the hash code for this export section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(exports);
    }
}
