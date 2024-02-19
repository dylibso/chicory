package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.io.WasmParseException;
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

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var tableCount = in.u31();
        tables.ensureCapacity(tables.size() + tableCount);

        // Parse individual tables in the tables section
        for (int i = 0; i < tableCount; i++) {
            var tableType = ValueType.refTypeForId(in.u8());
            var limitType = in.u32();
            if (limitType != 0x00 && limitType != 0x01) {
                throw new WasmParseException("Invalid limit type");
            }
            var min = in.u32Long();
            var limits = limitType > 0 ? new Limits(min, in.u32Long()) : new Limits(min);
            addTable(new Table(tableType, limits));
        }
    }
}
