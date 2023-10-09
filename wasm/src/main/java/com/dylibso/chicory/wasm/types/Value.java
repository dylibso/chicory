package com.dylibso.chicory.wasm.types;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Value {
    private final ValueType type;
    private final byte[] data;
    public static Value TRUE;
    public static Value FALSE;

    static {
        TRUE = Value.i32(1);
        FALSE = Value.i32(0);
    }

    public static Value fromFloat(float data) {
        return Value.f32((int) data);
    }

    public static Value fromDouble(double data) {
        return Value.f64((long) data);
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

    public ValueType getType() {
        return this.type;
    }

    public byte[] getData() {
        return this.data;
    }

    public Value(ValueType type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public Value(ValueType type, int data) {
        if (type != ValueType.I32 || type != ValueType.F32) {
            throw new IllegalArgumentException("Only use this constructor for 32 bit vals");
        }
        this.type = type;
        this.data = ByteBuffer.allocate(4).putInt(data).array();
    }

    public Value(ValueType type, long data) {
        this.type = type;
        switch (type) {
            case I32:
            case F32:
                {
                    this.data = new byte[4];
                    this.data[0] = (byte) (data >> 24);
                    this.data[1] = (byte) (data >> 16);
                    this.data[2] = (byte) (data >> 8);
                    this.data[3] = (byte) data;
                    break;
                }
            case I64:
            case F64:
                {
                    this.data = new byte[8];
                    this.data[0] = (byte) (data >> 56);
                    this.data[1] = (byte) (data >> 48);
                    this.data[2] = (byte) (data >> 40);
                    this.data[3] = (byte) (data >> 32);
                    this.data[4] = (byte) (data >> 24);
                    this.data[5] = (byte) (data >> 16);
                    this.data[6] = (byte) (data >> 8);
                    this.data[7] = (byte) data;
                    break;
                }
            default:
                this.data = new byte[] {};
                break;
        }
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
        return ByteBuffer.wrap(this.data).getLong();
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
        return (float) asInt();
    }

    public double asDouble() {
        return (double) asLong();
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
                throw new RuntimeException("TODO handle float");
        }
    }

    @Override
    public boolean equals(Object v) {
        if (v == this) return true;
        if (!(v instanceof Value)) return false;
        Value other = (Value) v;
        return type.equals(other.type) && data.equals(other.data);
    }
}
