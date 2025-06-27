package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.InvalidException;

/**
 * Limits for memory sizes, in pages.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/types.html#syntax-limits">Limits</a>
 * and <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-mem">Memories</a>
 * for reference.
 */
public final class MemoryLimits {

    /**
     * Maximum allowed number of pages.
     */
    public static final int MAX_PAGES = 1 << 16;

    private static final MemoryLimits DEFAULT_LIMITS = new MemoryLimits(0, MAX_PAGES);

    /**
     * Initial number of pages.
     */
    private final int initial;

    /**
     * Maximum number of pages.
     */
    private final int maximum;

    private final boolean shared;

    /**
     * Construct a new instance.
     * The maximum size will be {@link #MAX_PAGES}.
     *
     * @param initial the initial size, in pages
     */
    public MemoryLimits(int initial) {
        this(initial, MAX_PAGES);
    }

    /**
     * Construct a new instance.
     *
     * @param initial the initial size, in pages
     * @param maximum the maximum size, in pages
     */
    public MemoryLimits(int initial, int maximum) {
        this(initial, maximum, false);
    }

    /**
     * Construct a new instance.
     *
     * @param initial the initial size, in pages
     * @param maximum the maximum size, in pages
     * @param shared if this memory is shared
     */
    public MemoryLimits(int initial, int maximum, boolean shared) {
        if (initial > MAX_PAGES || maximum > MAX_PAGES || initial < 0 || maximum < 0) {
            throw new InvalidException("memory size must be at most 65536 pages (4GiB)");
        }
        if (initial > maximum) {
            throw new InvalidException("size minimum must not be greater than maximum");
        }

        this.initial = initial;
        this.maximum = maximum;
        this.shared = shared;
    }

    /**
     * @return the default memory limits
     */
    public static MemoryLimits defaultLimits() {
        return DEFAULT_LIMITS;
    }

    /**
     * @return the initial size, in pages
     */
    public int initialPages() {
        return initial;
    }

    /**
     * @return the maximum size, in pages
     */
    public int maximumPages() {
        return maximum;
    }

    public boolean shared() {
        return shared;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryLimits && equals((MemoryLimits) obj);
    }

    public boolean equals(MemoryLimits other) {
        return this == other
                || other != null && initial == other.initial && maximum == other.maximum;
    }

    @Override
    public int hashCode() {
        return maximum * 19 + initial;
    }

    @Override
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
        b.append(']');
        return b;
    }
}
