package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#whence">whence</a>
 */
final class WasiWhence {
    private WasiWhence() {}

    public static final int SET = 0;
    public static final int CUR = 1;
    public static final int END = 2;
}
