package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class Value {

    public static final Value TRUE;

    public static final Value FALSE;

    private final ValueType type;

    private final byte[] data;

    static {
        TRUE = Value.i32(1);
        FALSE = Value.i32(0);
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

    public Value(ValueType type, byte[] data) {
        this.type = requireNonNull(type, "type");
        this.data = requireNonNull(data, "data");
    }

    public Value(ValueType type, int value) {
        this(ensure32bitValueType(type), ByteBuffer.allocate(4).putInt(value).array());
    }

    public Value(ValueType type, long value) {
        this(requireNonNull(type, "type"), convertToBytes(type, value));
    }

    private static ValueType ensure32bitValueType(ValueType type) {
        if (ValueType.I32.equals(type) || ValueType.F32.equals(type)) {
            return type;
        }
        throw new IllegalArgumentException(
                "Invalid type for 32 bit value, only I32 or F32 are allowed, given: " + type);
    }

    private static byte[] convertToBytes(ValueType type, long value) {
        requireNonNull(type, "type");
        byte[] data;
        switch (type) {
            case I32:
            case F32:
                {
                    data = new byte[4];
                    data[0] = (byte) (value >> 24);
                    data[1] = (byte) (value >> 16);
                    data[2] = (byte) (value >> 8);
                    data[3] = (byte) value;
                    break;
                }
            case I64:
            case F64:
                {
                    data = new byte[8];
                    data[0] = (byte) (value >> 56);
                    data[1] = (byte) (value >> 48);
                    data[2] = (byte) (value >> 40);
                    data[3] = (byte) (value >> 32);
                    data[4] = (byte) (value >> 24);
                    data[5] = (byte) (value >> 16);
                    data[6] = (byte) (value >> 8);
                    data[7] = (byte) value;
                    break;
                }
            default:
                data = new byte[0];
                break;
        }
        return data;
    }

    // TODO memoize these
    public int asInt() {
        switch (type) {
            case I32:
            case F32:
                return ByteBuffer.wrap(this.data).getInt();
            case I64:
            case F64:
                return ByteBuffer.wrap(this.data, 4, 4).getInt();
        }
        ;
        throw new IllegalArgumentException("Can't turn wasm value of type " + type + " to a int");
    }

    // The unsigned representation of the int, stored in a long
    // so there are enough bits
    public long asUInt() {
        switch (type) {
            case I32:
            case F32:
                return ByteBuffer.wrap(this.data).getInt() & 0xFFFFFFFFL;
            case I64:
            case F64:
                return ByteBuffer.wrap(this.data, 4, 4).getInt() & 0xFFFFFFFFL;
        }
        ;
        throw new IllegalArgumentException("Can't turn wasm value of type " + type + " to a uint");
    }

    // TODO memoize these
    public long asLong() {
        return new BigInteger(this.data).longValue();
    }

    public BigInteger asULong() {
        var b = new BigInteger(this.data);
        if (b.signum() < 0) {
            return b.add(new BigInteger("2").pow(64));
        }
        return b;
    }

    // TODO memoize these
    public byte asByte() {
        return this.data[this.data.length - 1]; // this the right byte?
    }

    public short asShort() {
        switch (type) {
            case I32:
                return ByteBuffer.wrap(this.data, 2, 2).getShort();
            case I64:
                return ByteBuffer.wrap(this.data, 6, 2).getShort();
        }
        ;
        throw new IllegalArgumentException("Can't turn wasm value of type " + type + " to a short");
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
        return this.data;
    }

    public String toString() {
        switch (this.type) {
            case I32:
                {
                    return this.asInt() + "@i32";
                }
            case I64:
                {
                    return this.asLong() + "@i64";
                }
            case F32:
                {
                    return this.asFloat() + "@f32";
                }
            case F64:
                {
                    return this.asDouble() + "@f64";
                }
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
        return Objects.equals(type, other.type) && Arrays.equals(data, other.data);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(type, Arrays.hashCode(data));
    }
}
