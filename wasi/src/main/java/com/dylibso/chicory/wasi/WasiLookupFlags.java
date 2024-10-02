package com.dylibso.chicory.wasi;

/**
 * WASI <a href="https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#lookupflags">lookupflags</a> flags
 */
final class WasiLookupFlags {
    private WasiLookupFlags() {}

    public static final int SYMLINK_FOLLOW = 1;
}
