package com.dylibso.chicory.wasi;

// https://github.com/WebAssembly/WASI/blob/snapshot-01/phases/snapshot/docs.md#-eventtype-enumu8
public final class WasiEventType {
    private WasiEventType() {}

    public static final byte Clock = 0;
    public static final byte FdRead = 1;
    public static final byte FdWrite = 2;
}
