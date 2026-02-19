package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#clockid">clockid</a>
 */
final class WasiClockId {
    private WasiClockId() {}

    public static final int REALTIME = 0;
    public static final int MONOTONIC = 1;
    public static final int PROCESS_CPUTIME_ID = 2;
    public static final int THREAD_CPUTIME_ID = 3;
}
