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

    public static Limits unbounded() {
        return UNBOUNDED;
    }
}
