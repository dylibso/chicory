package com.dylibso.chicory.wasm.types;

public class ImportSection extends Section {
    private Import[] imports;

    public ImportSection(Import[] imports) {
        super(SectionId.IMPORT);
        this.imports = imports;
    }

    public Import[] imports() {
        return imports;
    }
}
