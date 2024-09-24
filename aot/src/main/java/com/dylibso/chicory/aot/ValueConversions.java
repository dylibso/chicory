package com.dylibso.chicory.aot;

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
        return Float.intBitsToFloat((int) val);
    }

    public static double toDouble(long val) {
        return Double.longBitsToDouble(val);
    }

    // To Long
    public static long asLong(int val) {
        return val;
    }

    public static long asLong(long val) {
        return val;
    }

    public static long asLong(float val) {
        return Float.floatToRawIntBits(val);
    }

    public static long asLong(double val) {
        return Double.doubleToLongBits(val);
    }
}
