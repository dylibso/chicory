package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.Value;

public final class ValueConversions {

    private ValueConversions() {}

    // From long
    public static int longToI32(long val) {
        return (int) val;
    }

    public static float longToF32(long val) {
        return Value.longToFloat(val);
    }

    public static double longToF64(long val) {
        return Value.longToDouble(val);
    }

    // To Long
    public static long i32ToLong(int val) {
        return val;
    }

    public static long f32ToLong(float val) {
        return Value.floatToLong(val);
    }

    public static long f64ToLong(double val) {
        return Value.doubleToLong(val);
    }
}
