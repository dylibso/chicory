package com.dylibso.chicory.runtime;

public class BitOps {

    public static long asUInt(long word) {
        return word & 0xFFFFFFFFL;
    }
}
