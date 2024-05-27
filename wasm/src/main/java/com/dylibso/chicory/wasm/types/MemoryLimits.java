package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.exceptions.InvalidException;

/**
 * Limits for memory sizes, in pages.
 * Memory limits also define whether the corresponding memory is <em>shared</em>.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/types.html#syntax-limits">Limits</a>
 * and <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-mem">Memories</a>
 * for reference.
 * See also <a href="https://github.com/WebAssembly/threads/blob/main/proposals/threads/Overview.md#spec-changes">Overview</a>
 * for the history of shared memory.
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
     * Whether the memory limits apply to a shared memory segment.
     */
    private final boolean shared;

    /**
     * Construct a new instance.
     * The maximum size will be {@link #MAX_PAGES} and {@code shared} will be {@code false}.
     *
     * @param initial the initial size
     */
    public MemoryLimits(int initial) {
        this(initial, MAX_PAGES, false);
    }

    /**
     * Construct a new instance.
     * {@code shared} will be {@code false}.
     *
     * @param initial the initial size
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
     * @param shared {@code true} if the limits apply to a shared memory segment, or {@code false} otherwise
     */
    public MemoryLimits(int initial, int maximum, boolean shared) {
        if (initial > MAX_PAGES || maximum > MAX_PAGES || initial < 0 || maximum < 0) {
            throw new InvalidException("memory size must be at most 65536 pages (4GiB)");
        } else if (initial > maximum) {
            throw new InvalidException("size minimum must not be greater than maximum");
        }

        this.initial = initial;
        this.maximum = maximum;
        this.shared = shared;
    }

    /**
     * {@return the default memory limits}
     */
    public static MemoryLimits defaultLimits() {
        return DEFAULT_LIMITS;
    }

    /**
     * {@return the initial size, in pages}
     */
    public int initialPages() {
        return initial;
    }

    /**
     * {@return the maximum size, in pages}
     */
    public int maximumPages() {
        return maximum;
    }

    /**
     * {@return <code>true</code> if the limits apply to a shared memory segment, or <code>false</code> otherwise}
     */
    public boolean shared() {
        return shared;
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
        b.append(']');
        if (shared) {
            b.append(":shared");
        }
        return b;
    }
}
