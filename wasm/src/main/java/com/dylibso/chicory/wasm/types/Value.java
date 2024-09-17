package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Value {

    public static final long TRUE = 1L;

    public static final long FALSE = 0L;

    public static final long REF_NULL_VALUE = -1L;
    public static final Value EXTREF_NULL = Value.externRef(REF_NULL_VALUE);
    public static final Value FUNCREF_NULL = Value.funcRef(REF_NULL_VALUE);

    public static final Value[] EMPTY_VALUES = new Value[0];

    private final ValueType type;

    private final long data;

    public static Value fromFloat(float data) {
        return Value.f32(Float.floatToRawIntBits(data));
    }

    public static long floatToLong(float data) {
        return Float.floatToRawIntBits(data);
    }

    public static Value fromDouble(double data) {
        return Value.f64(Double.doubleToRawLongBits(data));
    }

    public static long doubleToLong(double data) {
        return Double.doubleToRawLongBits(data);
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

    public Value(ValueType type, int value) {
        this.type = requireNonNull(ensure32bitValueType(type), "type");
        this.data = value;
    }

    public Value(ValueType type, long value) {
        this.type = requireNonNull(type, "type");
        data = value;
    }

    private static ValueType ensure32bitValueType(ValueType type) {
        switch (type) {
            case I32:
            case F32:
            case ExternRef:
            case FuncRef:
                return type;
            default:
                throw new IllegalArgumentException("Invalid type for 32 bit value: " + type);
        }
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

    // TODO memoize these
    public int asInt() {
        switch (type) {
            case I64:
            case F64:
            case I32:
            case F32:
                return (int) data;
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a int");
        }
    }

    // The unsigned representation of the int, stored in a long
    // so there are enough bits
    public long asUInt() {
        switch (type) {
            case I32:
            case F32:
            case I64:
            case F64:
                return data & 0xFFFFFFFFL;
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a uint");
        }
    }

    public long asLong() {
        switch (type) {
            case I32:
            case F32:
            case I64:
            case F64:
                return data;
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a long");
        }
    }

    public long raw() {
        return data;
    }

    // TODO memoize these
    public byte asByte() {
        switch (type) {
            case I32:
            case F32:
            case I64:
            case F64:
                return (byte) (data & 0xff);
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a byte");
        }
    }

    public short asShort() {
        switch (type) {
            case I32:
            case I64:
                return (short) (data & 0xffff);
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a short");
        }
    }

    public int asExtRef() {
        return (int) data;
    }

    public int asFuncRef() {
        return (int) data;
    }

    public float asFloat() {
        return Float.intBitsToFloat(asInt());
    }

    public static float longToFloat(long data) {
        return Float.intBitsToFloat((int) data);
    }

    public double asDouble() {
        return Double.longBitsToDouble(asLong());
    }

    public static double longToDouble(long data) {
        return Double.longBitsToDouble(data);
    }

    public ValueType type() {
        return type;
    }

    public byte[] data() {
        switch (type) {
            case I64:
            case F64:
                ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(data);
                return buffer.array();
            default:
                ByteBuffer buffer2 = ByteBuffer.allocate(4);
                buffer2.order(ByteOrder.LITTLE_ENDIAN);
                buffer2.putInt((int) data);
                return buffer2.array();
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case I32:
                return asInt() + "@i32";
            case I64:
                return asLong() + "@i64";
            case F32:
                return asFloat() + "@f32";
            case F64:
                return asDouble() + "@f64";
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
