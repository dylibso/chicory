package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TableSection extends Section {
    private final List<Table> tables;

    private TableSection(List<Table> tables) {
        super(SectionId.TABLE);
        this.tables = List.copyOf(tables);
    }

    public int tableCount() {
        return tables.size();
    }

    public Table getTable(int idx) {
        return tables.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Table> tables = new ArrayList<>();

        private Builder() {}

        /**
         * Add a table definition to this section.
         *
         * @param table the table to add to this section (must not be {@code null})
         * @return the Builder
         */
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
