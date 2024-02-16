package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public final class TableSection extends Section {
    private final ArrayList<Table> tables;

    /**
     * Construct a new, empty section instance.
     */
    public TableSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of functions to reserve space for
     */
    public TableSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private TableSection(ArrayList<Table> tables) {
        super(SectionId.TABLE);
        this.tables = tables;
    }

    public int tableCount() {
        return tables.size();
    }

    public Table getTable(int idx) {
        return tables.get(idx);
    }

    /**
     * Add a table definition to this section.
     *
     * @param table the table to add to this section (must not be {@code null})
     * @return the index of the newly-added table
     */
    public int addTable(Table table) {
        Objects.requireNonNull(table, "table");
        int idx = tables.size();
        tables.add(table);
        return idx;
    }
}
