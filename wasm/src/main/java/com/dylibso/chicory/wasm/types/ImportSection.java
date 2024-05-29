package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

public class ImportSection extends Section {
    private final ArrayList<Import> imports;

    /**
     * Construct a new, empty section instance.
     */
    public ImportSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of imports to reserve space for
     */
    public ImportSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private ImportSection(ArrayList<Import> imports) {
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

    /**
     * Add an import definition to this section.
     *
     * @param import_ the import to add to this section (must not be {@code null})
     * @return the index of the newly-added import
     */
    public int addImport(Import import_) {
        Objects.requireNonNull(import_, "import_");
        int idx = imports.size();
        imports.add(import_);
        return idx;
    }
}
