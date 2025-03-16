package com.dylibso.chicory.wasm.types.valuetypes;

/**
 * A separate holder class for constants.
 */
public final class ID {
    private ID() {}

    public static final int ExternRef = 0x6f;
    public static final int FuncRef = 0x70;
    public static final int V128 = 0x7b;
    public static final int F64 = 0x7c;
    public static final int F32 = 0x7d;
    public static final int I64 = 0x7e;
    public static final int I32 = 0x7f;
}
