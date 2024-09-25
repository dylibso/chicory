package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.Value;

public final class ValueConversions {

    private ValueConversions() {}

    // From long
    public static int toInt(long val) {
        return (int) val;
    }

    public static long toLong(long val) {
        return val;
    }

    public static float toFloat(long val) {
        return Value.longToFloat(val);
    }

    public static double toDouble(long val) {
        return Value.longToDouble(val);
    }

    // To Long
    public static long asLong(int val) {
        return val;
    }

    public static long asLong(long val) {
        return val;
    }

    public static long asLong(float val) {
        return Value.floatToLong(val);
    }

    public static long asLong(double val) {
        return Value.doubleToLong(val);
    }
}
