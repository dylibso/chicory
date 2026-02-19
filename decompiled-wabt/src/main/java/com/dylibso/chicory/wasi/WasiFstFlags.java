package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#fstflags">fstflags</a>
 */
final class WasiFstFlags {
    private WasiFstFlags() {}

    public static final int ATIM = bit(0);
    public static final int ATIM_NOW = bit(1);
    public static final int MTIM = bit(2);
    public static final int MTIM_NOW = bit(3);

    private static int bit(int n) {
        return 1 << n;
    }
}
