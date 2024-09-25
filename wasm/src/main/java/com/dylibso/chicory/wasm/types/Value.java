package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Value {

    public static final long TRUE = 1L;
    public static final long FALSE = 0L;
    public static final int REF_NULL_VALUE = -1;
    public static final Value EXTREF_NULL = Value.externRef(REF_NULL_VALUE);
    public static final Value FUNCREF_NULL = Value.funcRef(REF_NULL_VALUE);
    public static final long[] EMPTY_VALUES = new long[0];

    private final ValueType type;

    private final long data;

    public long raw() {
        return data;
    }

    public ValueType type() {
        return type;
    }

    public static long floatToLong(float data) {
        return Float.floatToRawIntBits(data);
    }

    public static float longToFloat(long data) {
        return Float.intBitsToFloat((int) data);
    }

    public static long doubleToLong(double data) {
        return Double.doubleToRawLongBits(data);
    }

    public static double longToDouble(long data) {
        return Double.longBitsToDouble(data);
    }

    public static Value fromFloat(float data) {
        return Value.f32(floatToLong(data));
    }

    public int asInt() {
        assert (type == ValueType.I32);
        return (int) data;
    }

    public long asLong() {
        assert (type == ValueType.I64);
        return data;
    }

    public float asFloat() {
        assert (type == ValueType.F32);
        return longToFloat(data);
    }

    public double asDouble() {
        assert (type == ValueType.F64);
        return longToDouble(data);
    }

    public static Value fromDouble(double data) {
        return Value.f64(doubleToLong(data));
    }

    public static Value i32(int data) {
        return i32((long) data);
    }

    public static Value i32(long data) {
        return new Value(ValueType.I32, data);
    }

    public static Value i64(long data) {
        return new Value(ValueType.I64, data);
    }

    public static Value f32(long data) {
        return new Value(ValueType.F32, data);
    }

    public static Value f64(long data) {
        return new Value(ValueType.F64, data);
    }

    public static Value externRef(long data) {
        return new Value(ValueType.ExternRef, data);
    }

    public static Value funcRef(long data) {
        return new Value(ValueType.FuncRef, data);
    }

    public Value(ValueType type, long value) {
        this.type = requireNonNull(type, "type");
        data = value;
    }

    /**
     * Create a zeroed value for the particular type.
     *
     * @param valueType must be a valid zeroable type.
     * @return a zero.
     */
    public static long zero(ValueType valueType) {
        switch (valueType) {
            case I32:
            case F32:
            case I64:
            case F64:
                return 0L;
            case FuncRef:
            case ExternRef:
                return REF_NULL_VALUE;
            default:
                throw new IllegalArgumentException(
                        "Can't create a zero value for type " + valueType);
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case I32:
                return ((int) data) + "@i32";
            case I64:
                return data + "@i64";
            case F32:
                return longToFloat(data) + "@f32";
            case F64:
                return longToDouble(data) + "@f64";
            case FuncRef:
                return "func[" + (int) data + "]";
            case ExternRef:
                return "ext[" + (int) data + "]";
            default:
                throw new AssertionError("Unhandled type: " + type);
        }
    }

    @Override
    public final boolean equals(Object v) {
        if (v == this) {
            return true;
        }
        if (!(v instanceof Value)) {
            return false;
        }
        Value other = (Value) v;
        return type.id() == other.type.id() && data == other.data;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type.id(), data);
    }
}
