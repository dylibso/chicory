package com.dylibso.chicory.wasm.types;

public class ImportSection extends Section {
    private Import[] imports;

    public ImportSection(long id, Import[] imports) {
        super(id);
        this.imports = imports;
    }

    public Import[] imports() {
        return imports;
    }
}
