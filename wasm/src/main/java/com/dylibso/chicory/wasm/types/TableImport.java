package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * An imported table.
 */
public final class TableImport extends Import {
    private final ValueType entryType;
    private final TableLimits limits;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported table name (must not be {@code null})
     * @param entryType the table entry type (must not be {@code null})
     * @param limits the table limits (must not be {@code null})
     */
    public TableImport(String moduleName, String name, ValueType entryType, TableLimits limits) {
        super(moduleName, name);
        this.entryType = Objects.requireNonNull(entryType, "entryType");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Returns the type of elements stored in this imported table.
     *
     * @return the table entry {@link ValueType}.
     */
    public ValueType entryType() {
        return entryType;
    }

    /**
     * Returns the size limits (initial and optional maximum) of this imported table.
     *
     * @return the {@link TableLimits}.
     */
    public TableLimits limits() {
        return limits;
    }

    /**
     * Returns the external type, which is always {@link ExternalType#TABLE}.
     *
     * @return {@link ExternalType#TABLE}.
     */
    @Override
    public ExternalType importType() {
        return ExternalType.TABLE;
    }

    /**
     * Compares this table import to another import.
     *
     * @param other the object to compare against.
     * @return {@code true} if the other object is a {@code TableImport} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Import other) {
        return other instanceof TableImport && equals((TableImport) other);
    }

    /**
     * Compares this table import to another table import for equality.
     * Equality is based on module name, name, entry type, and limits.
     *
     * @param other the {@code TableImport} to compare against.
     * @return {@code true} if the imports are equal, {@code false} otherwise.
     */
    public boolean equals(TableImport other) {
        return this == other
                || super.equals(other)
                        && entryType == other.entryType
                        && limits.equals(other.limits);
    }

    /**
     * Computes the hash code for this table import.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return (super.hashCode() * 19 + entryType.hashCode()) * 19 + limits.hashCode();
    }

    /**
     * Appends a string representation of this table import to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("table (type=").append(entryType).append(",limits=");
        limits.toString(b);
        b.append(')');
        return super.toString(b);
    }
}
