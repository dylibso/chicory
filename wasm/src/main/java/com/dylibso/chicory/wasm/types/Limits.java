package com.dylibso.chicory.wasm.types;

public class Limits {
    public static final long LIMIT_MAX = 0x1_0000_0000L;

    private static final Limits UNBOUNDED = new Limits(0);

    private final long min;
    private final long max;

    public Limits(long min) {
        this(min, LIMIT_MAX);
    }

    public Limits(long min, long max) {
        this.min = Math.min(Math.max(0, min), LIMIT_MAX);
        this.max = Math.min(Math.max(min, max), LIMIT_MAX);
    }

    public long min() {
        return min;
    }

    public long max() {
        return max;
    }

    public boolean equals(final Object obj) {
        return obj instanceof Limits && equals((Limits) obj);
    }

    public boolean equals(final Limits other) {
        return this == other || other != null && min == other.min && max == other.max;
    }

    public int hashCode() {
        return Long.hashCode(min) * 19 + Long.hashCode(max);
    }

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

    public static Limits unbounded() {
        return UNBOUNDED;
    }
}
