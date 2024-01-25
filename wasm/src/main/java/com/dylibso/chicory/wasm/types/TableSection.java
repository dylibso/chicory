package com.dylibso.chicory.wasm.types;

public class TableSection extends Section {
    private Table[] tables;

    public TableSection(Table[] tables) {
        super(SectionId.TABLE);
        this.tables = tables;
    }

    public Table[] tables() {
        return tables;
    }
}
