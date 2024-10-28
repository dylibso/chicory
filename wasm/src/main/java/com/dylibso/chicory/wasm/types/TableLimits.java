package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.WasmLimits.MAX_TABLE_ENTRIES;

import com.dylibso.chicory.wasm.InvalidException;

public class TableLimits {
    public static final long LIMIT_MAX = MAX_TABLE_ENTRIES;

    private static final TableLimits UNBOUNDED = new TableLimits(0);

    private final long min;
    private final long max;

    public TableLimits(long min) {
        this(min, LIMIT_MAX);
    }

    public TableLimits(long min, long max) {
        if (min > max) {
            throw new InvalidException("size minimum must not be greater than maximum");
        }
        this.min = Math.min(Math.max(0, min), LIMIT_MAX);
        this.max = Math.min(max, LIMIT_MAX);
    }

    public long min() {
        return min;
    }

    public long max() {
        return max;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TableLimits && equals((TableLimits) obj);
    }

    public boolean equals(TableLimits other) {
        return this == other || other != null && min == other.min && max == other.max;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(min) * 19 + Long.hashCode(max);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder b) {
        b.append("[").append(min).append(',');
        if (max == LIMIT_MAX) {
            b.append("max");
        } else {
            b.append(max);
        }
        return b.append(']');
    }

    public static TableLimits unbounded() {
        return UNBOUNDED;
    }
}
