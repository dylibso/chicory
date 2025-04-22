package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.WasmLimits.MAX_TABLE_ENTRIES;

import com.dylibso.chicory.wasm.InvalidException;

/**
 * Represents the limits (minimum and maximum size) for a WebAssembly Table.
 * These limits define the initial size and the potential growth capacity of a table.
 */
public class TableLimits {
    /**
     * The maximum number of entries allowed in a table as defined by the Wasm specification.
     * This value is used as the default maximum if none is specified.
     */
    public static final long LIMIT_MAX = MAX_TABLE_ENTRIES;

    private static final TableLimits UNBOUNDED = new TableLimits(0);

    private long min;
    private final long max;

    /**
     * Constructs new TableLimits with a specified minimum size and an implicit maximum size.
     * The maximum size defaults to {@link #LIMIT_MAX}.
     *
     * @param min the initial and minimum number of entries in the table.
     */
    public TableLimits(long min) {
        this(min, LIMIT_MAX);
    }

    /**
     * Constructs new TableLimits with specified minimum and maximum sizes.
     *
     * @param min the initial and minimum number of entries in the table.
     * @param max the maximum number of entries allowed in the table.
     * @throws InvalidException if the minimum size is greater than the maximum size.
     */
    public TableLimits(long min, long max) {
        if (min > max) {
            throw new InvalidException("size minimum must not be greater than maximum");
        }
        this.min = Math.min(Math.max(0, min), LIMIT_MAX);
        this.max = Math.min(max, LIMIT_MAX);
    }

    /**
     * Increases the current minimum size of the table by the specified amount.
     * This method is intended to reflect the runtime growth of the table.
     *
     * @param size the number of entries to add to the minimum size.
     */
    public void grow(int size) {
        min += size;
    }

    /**
     * Returns the minimum size of the table.
     *
     * @return the minimum number of table entries.
     */
    public long min() {
        return min;
    }

    /**
     * Returns the maximum size of the table.
     *
     * @return the maximum number of table entries.
     */
    public long max() {
        return max;
    }

    /**
     * Compares this table limits object to another object for equality.
     *
     * @param obj the object to compare against.
     * @return {@code true} if the object is a {@code TableLimits} instance with the same minimum and maximum values, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof TableLimits && equals((TableLimits) obj);
    }

    /**
     * Compares this table limits object to another {@code TableLimits} object for equality.
     *
     * @param other the {@code TableLimits} object to compare against.
     * @return {@code true} if both objects have the same minimum and maximum values, {@code false} otherwise.
     */
    public boolean equals(TableLimits other) {
        return this == other || other != null && min == other.min && max == other.max;
    }

    /**
     * Computes the hash code for this table limits object.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(min) * 19 + Long.hashCode(max);
    }

    /**
     * Returns a string representation of these table limits.
     *
     * @return a string in the format "[min,max]" or "[min,max]" if unbounded.
     */
    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Appends a string representation of these table limits to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    public StringBuilder toString(StringBuilder b) {
        b.append("[").append(min).append(',');
        if (max == LIMIT_MAX) {
            b.append("max");
        } else {
            b.append(max);
        }
        return b.append(']');
    }

    /**
     * Returns a shared instance representing unbounded table limits (min=0, max=LIMIT_MAX).
     *
     * @return an unbounded {@link TableLimits} instance.
     */
    public static TableLimits unbounded() {
        return UNBOUNDED;
    }
}
