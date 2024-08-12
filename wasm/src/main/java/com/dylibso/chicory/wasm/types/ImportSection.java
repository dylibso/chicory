package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ImportSection extends Section {
    private final List<Import> imports;

    private ImportSection(List<Import> imports) {
        super(SectionId.IMPORT);
        this.imports = imports;
    }

    public int importCount() {
        return imports.size();
    }

    public Import getImport(int idx) {
        return imports.get(idx);
    }

    public Stream<Import> stream() {
        return imports.stream();
    }

    public int count(ExternalType type) {
        return (int) imports.stream().filter(i -> i.importType() == type).count();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Import> imports = new ArrayList<>();

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

        public ImportSection build() {
            return new ImportSection(imports);
        }
    }
}
