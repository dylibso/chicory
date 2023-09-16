package com.dylibso.chicory.wasm.types;

public class TableSection extends Section {
    private Table[] tables;

    public TableSection(long id, long size, Table[] tables) {
       super(id, size);
       this.tables = tables;
    }

    public Table[] getTables() {
        return tables;
    }
}
