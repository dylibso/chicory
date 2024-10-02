package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#fdflags">fdflags</a>
 */
final class WasiFdFlags {
    private WasiFdFlags() {}

    public static final int APPEND = bit(0);
    public static final int DSYNC = bit(1);
    public static final int NONBLOCK = bit(2);
    public static final int RSYNC = bit(3);
    public static final int SYNC = bit(4);

    private static int bit(int n) {
        return 1 << n;
    }
}
