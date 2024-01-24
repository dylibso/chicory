package com.dylibso.chicory.wasm.types;

public class MemoryLimits {

    /**
     * Maximum allowed number of pages.
     */
    public static final int MAX_PAGES = 2 << 15;

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
     * Default memory limits.
     * @return
     */
    public static MemoryLimits defaultLimits() {
        return new MemoryLimits(0, MAX_PAGES);
    }

    public int initialPages() {
        return initial;
    }

    public int maximumPages() {
        return maximum;
    }
}
