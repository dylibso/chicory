package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Value {

    public static final Value TRUE;

    public static final Value FALSE;

    public static final Value REF_NULL;

    public static final Value[] EMPTY_VALUES = new Value[0];

    public static final BigInteger TWO_POW_64 = new BigInteger("2").pow(64);

    private final ValueType type;

    private long data;

    static {
        TRUE = Value.i32(1);
        FALSE = Value.i32(0);
        REF_NULL = Value.externRef(1L << 31);
    }

    public static Value fromFloat(float data) {
        return Value.f32(Float.floatToIntBits(data));
    }

    public static Value fromDouble(double data) {
        return Value.f64(Double.doubleToLongBits(data));
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

    public Value(ValueType type, int value) {
        this.type = requireNonNull(ensure32bitValueType(type), "type");
        this.data = value;
    }

    public Value(ValueType type, long value) {
        this.type = requireNonNull(type, "type");
        data = value;
    }

    private static ValueType ensure32bitValueType(ValueType type) {
        if (ValueType.I32.equals(type) || ValueType.F32.equals(type)) {
            return type;
        }
        throw new IllegalArgumentException(
                "Invalid type for 32 bit value, only I32 or F32 are allowed, given: " + type);
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

    public BigInteger asULong() {
        BigInteger b;
        switch (type) {
            case I32:
            case F32:
            case I64:
            case F64:
                b = new BigInteger(Long.toString(data));
                break;
            default:
                throw new IllegalArgumentException(
                        "Can't turn wasm value of type " + type + " to a ulong");
        }
        if (b.signum() < 0) {
            return b.add(TWO_POW_64);
        }
        return b;
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

    public float asFloat() {
        return Float.intBitsToFloat(asInt());
    }

    public double asDouble() {
        return Double.longBitsToDouble(asLong());
    }

    public ValueType getType() {
        return this.type;
    }

    public byte[] getData() {
        switch (this.type) {
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

    public String toString() {
        switch (this.type) {
            case I32:
                return this.asInt() + "@i32";
            case I64:
                return this.asLong() + "@i64";
            case F32:
                return this.asFloat() + "@f32";
            case F64:
                return this.asDouble() + "@f64";
            case FuncRef:
                return "func";
            case ExternRef:
                return "ext";
            default:
                throw new RuntimeException("TODO handle missing types");
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
        return Objects.equals(type, other.type) && data == other.data;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type, data);
    }
}
