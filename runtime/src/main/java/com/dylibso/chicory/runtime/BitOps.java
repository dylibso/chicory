package com.dylibso.chicory.runtime;

public class BitOps {

    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static byte asByte(long word) {
        return (byte) (word & 0xFF);
    }

    public static long asUInt(long word) {
        return word & 0xFFFFFFFFL;
    }
}
