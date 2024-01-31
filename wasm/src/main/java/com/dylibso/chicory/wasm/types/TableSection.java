package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class TableSection extends Section {
    private final ArrayList<Table> tables;

    private TableSection(ArrayList<Table> tables) {
        super(SectionId.TABLE);
        this.tables = tables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TableSection tableSection) {
        return new Builder(tableSection);
    }

    public int tableCount() {
        return tables.size();
    }

    public Table getTable(int idx) {
        return tables.get(idx);
    }

    public static final class Builder {
        private final ArrayList<Table> tables;

        private Builder() {
            this.tables = new ArrayList<>();
        }

        private Builder(TableSection tableSection) {
            this.tables = new ArrayList<>();
            this.tables.addAll(tableSection.tables);
        }

        public Builder addTable(Table table) {
            Objects.requireNonNull(table, "table");
            tables.add(table);
            return this;
        }

        public TableSection build() {
            return new TableSection(tables);
        }
    }
}
