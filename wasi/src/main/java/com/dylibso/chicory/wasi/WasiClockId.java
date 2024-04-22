package com.dylibso.chicory.wasi;

final class WasiClockId {
    private WasiClockId() {}

    public static final int REALTIME = 0;
    public static final int MONOTONIC = 1;
    public static final int PROCESS_CPUTIME_ID = 2;
    public static final int THREAD_CPUTIME_ID = 3;
}
