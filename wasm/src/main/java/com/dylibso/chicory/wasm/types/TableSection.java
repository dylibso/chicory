package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Table Section in a WebAssembly module.
 * This section declares all tables defined within the module, excluding imported tables.
 */
public final class TableSection extends Section {
    private final List<Table> tables;

    private TableSection(List<Table> tables) {
        super(SectionId.TABLE);
        this.tables = List.copyOf(tables);
    }

    /**
     * Returns the number of tables defined in this section.
     *
     * @return the count of tables.
     */
    public int tableCount() {
        return tables.size();
    }

    /**
     * Returns the table definition at the specified index.
     *
     * @param idx the index of the table to retrieve.
     * @return the {@link Table} definition at the given index.
     */
    public Table getTable(int idx) {
        return tables.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link TableSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link TableSection} instances.
     */
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

        /**
         * Constructs the {@link TableSection} instance from the added tables.
         *
         * @return the built {@link TableSection}.
         */
        public TableSection build() {
            return new TableSection(tables);
        }
    }

    /**
     * Compares this table section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code TableSection} with the same tables, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof TableSection)) {
            return false;
        }
        TableSection that = (TableSection) o;
        return Objects.equals(tables, that.tables);
    }

    /**
     * Computes the hash code for this table section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(tables);
    }
}
