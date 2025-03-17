package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Value {

    public static final long TRUE = 1L;
    public static final long FALSE = 0L;
    public static final int REF_NULL_VALUE = -1;
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

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static byte[] vecTo8(long[] values) {
        var result = new byte[values.length * 8];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (byte) (v & 0xFFL);
            result[++i] = (byte) ((v >> 8) & 0xFFL);
            result[++i] = (byte) ((v >> 16) & 0xFFL);
            result[++i] = (byte) ((v >> 24) & 0xFFL);
            result[++i] = (byte) ((v >> 32) & 0xFFL);
            result[++i] = (byte) ((v >> 40) & 0xFFL);
            result[++i] = (byte) ((v >> 48) & 0xFFL);
            result[++i] = (byte) ((v >> 56) & 0xFFL);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] bytesToVec(byte[] bytes) {
        var result = new long[bytes.length / 8];
        var valueIdx = 0;
        for (int i = 0; i < bytes.length; i++) {
            result[valueIdx++] =
                    Byte.toUnsignedLong(bytes[i]) + (Byte.toUnsignedLong(bytes[++i]) << 8L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 16L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 24L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 32L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 40L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 48L)
                            | (Byte.toUnsignedLong(bytes[++i]) << 56L);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static int[] vecTo16(long[] values) {
        var result = new int[values.length * 4];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (int) (v & 0xFFFFL);
            result[++i] = (int) ((v >> 16) & 0xFFFFL);
            result[++i] = (int) ((v >> 32) & 0xFFFFL);
            result[++i] = (int) ((v >> 48) & 0xFFFFL);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] vecTo32(long[] values) {
        var result = new long[values.length * 2];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = (v & 0xFFFFFFFFL);
            result[++i] = ((v >> 32) & 0xFFFFFFFFL);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static float[] vecToF32(long[] values) {
        var result = new float[values.length * 2];
        var valueIdx = 0;
        for (int i = 0; i < result.length; i++) {
            var v = values[valueIdx++];
            result[i] = Float.intBitsToFloat((int) (v & 0xFFFFFFFFL));
            result[++i] = Float.intBitsToFloat((int) ((v >> 32) & 0xFFFFFFFFL));
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static double[] vecToF64(long[] values) {
        var result = new double[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.longBitsToDouble(values[i]);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] i8ToVec(long[]... vec) {
        var result = new long[vec.length * 2];
        for (int i = 0; i < result.length; ) {
            long[] v = vec[i];
            result[i++] =
                    (v[0] & 0xFF)
                            | ((v[1] & 0xFF) << 8)
                            | ((v[2] & 0xFF) << 16)
                            | ((v[3] & 0xFF) << 24)
                            | ((v[4] & 0xFF) << 32)
                            | ((v[5] & 0xFF) << 40)
                            | ((v[6] & 0xFF) << 48)
                            | ((v[7] & 0xFF) << 56);
            result[i++] =
                    (v[8] & 0xFF)
                            | ((v[9] & 0xFF) << 8)
                            | ((v[10] & 0xFF) << 16)
                            | ((v[11] & 0xFF) << 24)
                            | ((v[12] & 0xFF) << 32)
                            | ((v[13] & 0xFF) << 40)
                            | ((v[14] & 0xFF) << 48)
                            | ((v[15] & 0xFF) << 56);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] i16ToVec(long[]... vec) {
        var result = new long[vec.length * 2];
        for (int i = 0; i < result.length; ) {
            long[] v = vec[i];
            result[i++] =
                    (v[0] & 0xFFFF)
                            | ((v[1] & 0xFFFF) << 16)
                            | ((v[2] & 0xFFFF) << 32)
                            | ((v[3] & 0xFFFF) << 48);
            result[i++] =
                    (v[4] & 0xFFFF)
                            | ((v[5] & 0xFFFF) << 16)
                            | ((v[6] & 0xFFFF) << 32)
                            | ((v[7] & 0xFFFF) << 48);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] i32ToVec(long[]... vec) {
        var result = new long[vec.length * 2];
        for (int i = 0; i < result.length; ) {
            long[] v = vec[i];
            result[i++] = (v[1] & 0xFFFF_FFFFL) << 32 | (v[0] & 0xFFFF_FFFFL);
            result[i++] = (v[3] & 0xFFFF_FFFFL) << 32 | (v[2] & 0xFFFF_FFFFL);
        }
        return result;
    }

    // This is really only a convenience method to concat the vectors.
    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static long[] i64ToVec(long[]... vec) {
        var result = new long[vec.length * 2];
        for (int i = 0; i < result.length; ) {
            long[] v = vec[i];
            result[i++] = v[0];
            result[i++] = v[1];
        }
        return result;
    }

    // Since the longs are already converted, this is just a wrapper for i32ToVec
    public static long[] f32ToVec(long[]... vec) {
        return i32ToVec(vec);
    }

    // Since the longs are already converted, this is just a wrapper for i64ToVec
    public static long[] f64ToVec(long[]... vec) {
        return i64ToVec(vec);
    }

    /**
     * Create a zeroed value for the particular type.
     *
     * @param valueType must be a valid zeroable type.
     * @return a zero.
     */
    public static long zero(ValueType valueType) {
        switch (valueType.opcode()) {
            case ValueType.ID.I32:
            case ValueType.ID.F32:
            case ValueType.ID.I64:
            case ValueType.ID.F64:
                return 0L;
            case ValueType.ID.ExnRef:
            case ValueType.ID.Ref:
            case ValueType.ID.RefNull:
                return REF_NULL_VALUE;
            default:
                throw new IllegalArgumentException(
                        "Can't create a zero value for type " + valueType);
        }
    }

    @Override
    public String toString() {
        switch (type.opcode()) {
            case ValueType.ID.I32:
                return ((int) data) + "@i32";
            case ValueType.ID.I64:
                return data + "@i64";
            case ValueType.ID.F32:
                return longToFloat(data) + "@f32";
            case ValueType.ID.F64:
                return longToDouble(data) + "@f64";
            case ValueType.ID.V128:
                return data + "@v128";
            case ValueType.ID.Ref:
                return "ref[" + (int) data + "]";
            case ValueType.ID.RefNull:
                return "refnull[" + (int) data + "]";
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
