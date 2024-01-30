package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class TableSection extends Section {
    private final ArrayList<Table> tables;

    public TableSection(Table[] tables) {
        super(SectionId.TABLE);
        this.tables = new ArrayList<>(List.of(tables));
    }

    public int tableCount() {
        return tables.size();
    }

    public Table getTable(int idx) {
        return tables.get(idx);
    }
}
