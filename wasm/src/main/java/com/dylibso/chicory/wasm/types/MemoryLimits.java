package com.dylibso.chicory.wasm.types;

public class MemoryLimits {

    /**
     * Maximum allowed number of pages.
     */
    public static final int MAX_PAGES = 2 << 15;

    private static final MemoryLimits DEFAULT_LIMITS = new MemoryLimits(0, MAX_PAGES);

    /**
     * Initial number of pages.
     */
    private final int initial;

    /**
     * Maximum number of pages.
     */
    private final int maximum;

    public MemoryLimits(int initial) {
        this(initial, MAX_PAGES);
    }

    public MemoryLimits(int initial, int maximum) {
        if (initial < 0 || initial > maximum) {
            throw new IllegalArgumentException(
                    "initial must be >= 0 and <= maximum, but was " + initial);
        }

        if (maximum > MAX_PAGES) {
            throw new IllegalArgumentException("maximum must be <= MAX_PAGES, but was " + maximum);
        }

        this.initial = initial;
        this.maximum = maximum;
    }

    /**
     * {@return the default memory limits}
     */
    public static MemoryLimits defaultLimits() {
        return DEFAULT_LIMITS;
    }

    public int initialPages() {
        return initial;
    }

    public int maximumPages() {
        return maximum;
    }

    public boolean equals(final Object obj) {
        return obj instanceof MemoryLimits && equals((MemoryLimits) obj);
    }

    public boolean equals(final MemoryLimits other) {
        return this == other
                || other != null && initial == other.initial && maximum == other.maximum;
    }

    public int hashCode() {
        return maximum * 19 + initial;
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder b) {
        b.append("[").append(initial).append(',');
        if (maximum == MAX_PAGES) {
            b.append("max");
        } else {
            b.append(maximum);
        }
        return b.append(']');
    }
}
