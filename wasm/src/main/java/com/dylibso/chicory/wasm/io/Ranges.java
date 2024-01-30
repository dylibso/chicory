package com.dylibso.chicory.wasm.io;

/**
 * Ranges of various numeric types.
 */
public final class Ranges {
    public static final int U8_MIN = 0;
    public static final int U8_MAX = Integer.MAX_VALUE >> 23;

    public static final int U16_MIN = 0;
    public static final int U16_MAX = Integer.MAX_VALUE >> 15;

    public static final int U31_MIN = 0;
    public static final int U31_MAX = Integer.MAX_VALUE;

    public static final long U32_MIN = 0;
    public static final long U32_MAX = Long.MAX_VALUE >> 31;

    public static final long U64_MIN = 0;
    public static final long U64_MAX = -1;

    public static final int S32_MIN = Integer.MIN_VALUE;
    public static final int S32_MAX = U31_MAX;

    public static final long S33_MIN = Long.MIN_VALUE >> 31;
    public static final long S33_MAX = U32_MAX;

    public static final long S64_MIN = Long.MIN_VALUE;
    public static final long S64_MAX = Long.MAX_VALUE;

    private Ranges() {}
}
