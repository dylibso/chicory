package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * Represents a table definition in a WebAssembly module.
 * Tables are arrays of opaque values, typically function references (funcref)
 * or external references (externref).
 */
public class Table {
    private final ValueType elementType;
    private final TableLimits limits;

    /**
     * Constructs a new Table definition.
     *
     * @param elementType the type of elements stored in the table (must be a reference type).
     * @param limits the size limits of the table.
     * @throws IllegalArgumentException if the element type is not a reference type.
     */
    public Table(ValueType elementType, TableLimits limits) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Returns the type of elements stored in this table.
     *
     * @return the element {@link ValueType}.
     */
    public ValueType elementType() {
        return elementType;
    }

    /**
     * Returns the size limits (initial and optional maximum) of this table.
     *
     * @return the {@link TableLimits}.
     */
    public TableLimits limits() {
        return limits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Table)) {
            return false;
        }
        Table table = (Table) o;
        return elementType == table.elementType && Objects.equals(limits, table.limits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, limits);
    }
}
