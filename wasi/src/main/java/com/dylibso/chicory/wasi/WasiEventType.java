package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#eventtype">eventtype</a>
 */
final class WasiEventType {
    private WasiEventType() {}

    public static final byte CLOCK = 0;
    public static final byte FD_READ = 1;
    public static final byte FD_WRITE = 2;
}
