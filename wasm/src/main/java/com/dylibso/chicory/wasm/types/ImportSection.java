package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

public class ImportSection extends Section {
    private final ArrayList<Import> imports;

    private ImportSection(ArrayList<Import> imports) {
        super(SectionId.IMPORT);
        this.imports = imports;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ImportSection importSection) {
        return new Builder(importSection);
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

    public static final class Builder {
        private final ArrayList<Import> imports;

        private Builder() {
            this.imports = new ArrayList<>();
        }

        private Builder(ImportSection importSection) {
            this.imports = new ArrayList<>();
            this.imports.addAll(importSection.imports);
        }

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
