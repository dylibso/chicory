package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#oflags">oflags</a>
 */
final class WasiOpenFlags {
    private WasiOpenFlags() {}

    public static final int CREAT = bit(0);
    public static final int DIRECTORY = bit(1);
    public static final int EXCL = bit(2);
    public static final int TRUNC = bit(3);

    private static int bit(int n) {
        return 1 << n;
    }
}
