package com.dylibso.chicory.wasm.types;

public class ImportSection extends Section {
    private Import[] imports;

    public ImportSection(long id, long size, Import[] imports) {
        super(id, size);
        this.imports = imports;
    }

    public Import[] imports() {
        return imports;
    }
}
