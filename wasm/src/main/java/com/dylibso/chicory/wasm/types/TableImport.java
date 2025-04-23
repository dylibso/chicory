package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * An imported table.
 */
public final class TableImport extends Import {
    private final ValType entryType;
    private final TableLimits limits;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported table name (must not be {@code null})
     * @param entryType the table entry type (must not be {@code null})
     * @param limits the table limits (must not be {@code null})
     */
    public TableImport(String moduleName, String name, ValType entryType, TableLimits limits) {
        super(moduleName, name);
        this.entryType = Objects.requireNonNull(entryType, "entryType");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Deprecated(since = "23/05/2025", forRemoval = true)
    public TableImport(String moduleName, String name, ValueType entryType, TableLimits limits) {
        super(moduleName, name);
        this.entryType = Objects.requireNonNull(entryType, "entryType").toNew();
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * @return the table entry type
     */
    public ValType entryType() {
        return entryType;
    }

    /**
     * @return the table size limits
     */
    public TableLimits limits() {
        return limits;
    }

    @Override
    public ExternalType importType() {
        return ExternalType.TABLE;
    }

    @Override
    public boolean equals(Import other) {
        return other instanceof TableImport && equals((TableImport) other);
    }

    public boolean equals(TableImport other) {
        return this == other
                || super.equals(other)
                        && entryType == other.entryType
                        && limits.equals(other.limits);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 19 + entryType.hashCode()) * 19 + limits.hashCode();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("table (type=").append(entryType).append(",limits=");
        limits.toString(b);
        b.append(')');
        return super.toString(b);
    }
}
