package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents the Import Section in a WebAssembly module.
 * This section declares all entities (functions, tables, memories, globals, tags) imported from other modules.
 */
public final class ImportSection extends Section {
    private final List<Import> imports;

    private ImportSection(List<Import> imports) {
        super(SectionId.IMPORT);
        this.imports = List.copyOf(imports);
    }

    /**
     * Returns the total number of imports defined in this section.
     *
     * @return the count of imports.
     */
    public int importCount() {
        return imports.size();
    }

    /**
     * Returns the import definition at the specified index.
     *
     * @param idx the index of the import to retrieve.
     * @return the {@link Import} definition at the given index.
     */
    public Import getImport(int idx) {
        return imports.get(idx);
    }

    /**
     * Returns a stream over the imports in this section.
     *
     * @return a {@link Stream} of {@link Import} instances.
     */
    public Stream<Import> stream() {
        return imports.stream();
    }

    /**
     * Returns the number of imports of a specific {@link ExternalType}.
     *
     * @param type the type of import to count.
     * @return the count of imports matching the specified type.
     */
    public int count(ExternalType type) {
        return (int) imports.stream().filter(i -> i.importType() == type).count();
    }

    /**
     * Creates a new builder for constructing an {@link ImportSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ImportSection} instances.
     */
    public static final class Builder {
        private final List<Import> imports = new ArrayList<>();

        private Builder() {}

        /**
         * Add an import definition to this section.
         *
         * @param import_ the import to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addImport(Import import_) {
            Objects.requireNonNull(import_, "import_");
            imports.add(import_);
            return this;
        }

        /**
         * Constructs the {@link ImportSection} instance from the added imports.
         *
         * @return the built {@link ImportSection}.
         */
        public ImportSection build() {
            return new ImportSection(imports);
        }
    }

    /**
     * Compares this import section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is an {@code ImportSection} with the same imports, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ImportSection)) {
            return false;
        }
        ImportSection that = (ImportSection) o;
        return Objects.equals(imports, that.imports);
    }

    /**
     * Computes the hash code for this import section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(imports);
    }
}
