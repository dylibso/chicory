package com.dylibso.chicory.wasm.types;

public class TableSection extends Section {
    private Table[] tables;

    public TableSection(long id, Table[] tables) {
        super(id);
        this.tables = tables;
    }

    public Table[] tables() {
        return tables;
    }
}
