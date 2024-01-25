package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ImportSection extends Section {
    private final ArrayList<Import> imports;

    public ImportSection(Import[] imports) {
        super(SectionId.IMPORT);
        this.imports = new ArrayList<>(List.of(imports));
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
}
