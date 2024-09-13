package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Value {

    public static final Value TRUE = Value.i32(1);

    public static final Value FALSE = Value.i32(0);

    public static final int REF_NULL_VALUE = -1;
    public static final Value EXTREF_NULL = Value.externRef(REF_NULL_VALUE);
    public static final Value FUNCREF_NULL = Value.funcRef(REF_NULL_VALUE);

    public static final Value[] EMPTY_VALUES = new Value[0];

    private final ValueType type;

    private final long data;

    public static Value fromFloat(float data) {
        return Value.f32(Float.floatToRawIntBits(data));
    }

    public static Value fromDouble(double data) {
        return Value.f64(Double.doubleToRawLongBits(data));
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

    public static Value v128(long data) {
        return new Value(ValueType.V128, data);
    }

    public static Value externRef(int data) {
        return new Value(ValueType.ExternRef, data);
    }

    public static Value funcRef(int data) {
        return new Value(ValueType.FuncRef, data);
    }

    public static byte[] vecTo8(Value[] values) {
        var result = new byte[values.length * 8];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (byte) (v.data & 0xFFL);
            result[++i] = (byte) ((v.data & 0xFF00L) >> 8);
            result[++i] = (byte) ((v.data & 0xFF0000L) >> 16);
            result[++i] = (byte) ((v.data & 0xFF000000L) >> 24);
            result[++i] = (byte) ((v.data & 0xFF00000000L) >> 32);
            result[++i] = (byte) ((v.data & 0xFF0000000000L) >> 40);
            result[++i] = (byte) ((v.data & 0xFF000000000000L) >> 48);
            result[++i] = (byte) ((v.data & 0xFF00000000000000L) >> 56);
        }
        return result;
    }

    public static int[] vecTo16(Value[] values) {
        var result = new int[values.length * 4];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (int) (v.data & 0xFFFFL);
            result[++i] = (int) ((v.data >> 16) & 0xFFFFL);
            result[++i] = (int) ((v.data >> 32) & 0xFFFFL);
            result[++i] = (int) ((v.data >> 48) & 0xFFFFL);
        }
        return result;
    }

    public static long[] vecTo32(Value[] values) {
        var result = new long[values.length * 2];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (v.data & 0xFFFFFFFFL);
            result[++i] = ((v.data >> 32) & 0xFFFFFFFFL);
        }
        return result;
    }

    public static float[] vecToFloatArray(Value[] values) {
        var result = new float[values.length];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = v.asFloat();
        }
        return result;
    }

    public Value(ValueType type, int value) {
        this.type = requireNonNull(ensure32bitValueType(type), "type");
        this.data = value;
    }

    public Value(ValueType type, long value) {
        this.type = requireNonNull(type, "type");
        data = value;
    }

    public boolean isVec() {
        return this.type == ValueType.V128;
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
    public static Value zero(ValueType valueType) {
        switch (valueType) {
            case I32:
                return Value.i32(0);
            case F32:
                return Value.f32(0);
            case I64:
                return Value.i64(0);
            case F64:
                return Value.f64(0);
            case FuncRef:
                return Value.FUNCREF_NULL;
            case ExternRef:
                return Value.EXTREF_NULL;
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
            case V128:
                return data;
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a long");
        }
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

    public double asDouble() {
        return Double.longBitsToDouble(asLong());
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
