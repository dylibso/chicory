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
        if (initial > MAX_PAGES || maximum > MAX_PAGES || initial < 0 || maximum < 0) {
            throw new InvalidException("memory size must be at most 65536 pages (4GiB)");
        }
        if (initial > maximum) {
            throw new InvalidException("size minimum must not be greater than maximum");
        }

        this.initial = initial;
        this.maximum = maximum;
    }

    /**
     * Returns the default memory limits (0 initial, {@link #MAX_PAGES} maximum).
     *
     * @return the default {@link MemoryLimits} instance.
     */
    public static MemoryLimits defaultLimits() {
        return DEFAULT_LIMITS;
    }

    /**
     * Returns the initial size of the memory, in pages.
     *
     * @return the initial size in WebAssembly pages (64KiB each).
     */
    public int initialPages() {
        return initial;
    }

    /**
     * Returns the maximum allowed size of the memory, in pages.
     *
     * @return the maximum size in WebAssembly pages (64KiB each).
     */
    public int maximumPages() {
        return maximum;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryLimits && equals((MemoryLimits) obj);
    }

    /**
     * Compares this memory limits object to another {@code MemoryLimits} object for equality.
     *
     * @param other the {@code MemoryLimits} object to compare against.
     * @return {@code true} if both objects have the same initial and maximum values, {@code false} otherwise.
     */
    public boolean equals(MemoryLimits other) {
        return this == other
                || other != null && initial == other.initial && maximum == other.maximum;
    }

    /**
     * Computes the hash code for this memory limits object.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return maximum * 19 + initial;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Appends a string representation of these memory limits to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
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
