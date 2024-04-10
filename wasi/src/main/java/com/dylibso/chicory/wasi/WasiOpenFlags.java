package com.dylibso.chicory.wasi;

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
