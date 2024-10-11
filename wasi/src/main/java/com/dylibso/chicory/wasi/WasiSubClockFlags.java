package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#subclockflags">subclockflags</a> flags
 */
final class WasiSubClockFlags {
    private WasiSubClockFlags() {}

    public static final int SUBSCRIPTION_CLOCK_ABSTIME = 1;
}
