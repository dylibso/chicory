package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Represents a WebAssembly value, which can be one of the numeric types
 * (I32, I64, F32, F64) or a reference type (FuncRef, ExternRef).
 * Internally, all values are stored as a 64-bit long (`data`), regardless of the actual {@link ValueType}.
 * Specific methods are provided to convert this raw long representation to the appropriate Java type
 * (e.g., {@link #asInt()}, {@link #asFloat()}) and vice-versa (e.g., {@link #i32(int)}, {@link #fromFloat(float)}).
 */
public class Value {

    /** Constant representing the Wasm boolean true value (1). */
    public static final long TRUE = 1L;

    /** Constant representing the Wasm boolean false value (0). */
    public static final long FALSE = 0L;

    /** Constant representing the null reference value for reference types. */
    public static final int REF_NULL_VALUE = -1;

    /** Constant representing an empty array of values. */
    public static final long[] EMPTY_VALUES = new long[0];

    private final ValueType type;

    private final long data;

    /**
     * Returns the raw 64-bit representation of this value.
     * For F32, the lower 32 bits contain the float bits. For I32, the lower 32 bits contain the int bits.
     *
     * @return the raw long data.
     */
    public long raw() {
        return data;
    }

    /**
     * Returns the {@link ValueType} of this value.
     *
     * @return the type of the value.
     */
    public ValueType type() {
        return type;
    }

    /**
     * Converts a Java float to its raw 32-bit integer representation, stored in a long.
     * The upper 32 bits of the long are unused/zero.
     *
     * @param data the float value.
     * @return the raw integer bits of the float, as a long.
     */
    public static long floatToLong(float data) {
        return Float.floatToRawIntBits(data);
    }

    /**
     * Converts a raw 32-bit integer representation (stored in a long) back to a Java float.
     * Only the lower 32 bits of the long are used.
     *
     * @param data the raw integer bits (lower 32 bits of the long).
     * @return the float value.
     */
    public static float longToFloat(long data) {
        return Float.intBitsToFloat((int) data);
    }

    /**
     * Converts a Java double to its raw 64-bit long representation.
     *
     * @param data the double value.
     * @return the raw long bits of the double.
     */
    public static long doubleToLong(double data) {
        return Double.doubleToRawLongBits(data);
    }

    /**
     * Converts a raw 64-bit long representation back to a Java double.
     *
     * @param data the raw long bits.
     * @return the double value.
     */
    public static double longToDouble(long data) {
        return Double.longBitsToDouble(data);
    }

    /**
     * Creates a new F32 {@link Value} from a Java float.
     *
     * @param data the float value.
     * @return a new {@link Value} of type F32.
     */
    public static Value fromFloat(float data) {
        return Value.f32(floatToLong(data));
    }

    /**
     * Returns the value interpreted as a Java int.
     * Asserts that the value type is I32.
     *
     * @return the int value.
     */
    public int asInt() {
        assert (type == ValueType.I32);
        return (int) data;
    }

    /**
     * Returns the value interpreted as a Java long.
     * Asserts that the value type is I64.
     *
     * @return the long value.
     */
    public long asLong() {
        assert (type == ValueType.I64);
        return data;
    }

    /**
     * Returns the value interpreted as a Java float.
     * Asserts that the value type is F32.
     *
     * @return the float value.
     */
    public float asFloat() {
        assert (type == ValueType.F32);
        return longToFloat(data);
    }

    /**
     * Returns the value interpreted as a Java double.
     * Asserts that the value type is F64.
     *
     * @return the double value.
     */
    public double asDouble() {
        assert (type == ValueType.F64);
        return longToDouble(data);
    }

    /**
     * Creates a new F64 {@link Value} from a Java double.
     *
     * @param data the double value.
     * @return a new {@link Value} of type F64.
     */
    public static Value fromDouble(double data) {
        return Value.f64(doubleToLong(data));
    }

    /**
     * Creates a new I32 {@link Value} from a Java int.
     *
     * @param data the int value.
     * @return a new {@link Value} of type I32.
     */
    public static Value i32(int data) {
        return i32((long) data);
    }

    /**
     * Creates a new I32 {@link Value} from a raw long value (lower 32 bits used).
     *
     * @param data the raw long value.
     * @return a new {@link Value} of type I32.
     */
    public static Value i32(long data) {
        return new Value(ValueType.I32, data);
    }

    /**
     * Creates a new I64 {@link Value} from a Java long.
     *
     * @param data the long value.
     * @return a new {@link Value} of type I64.
     */
    public static Value i64(long data) {
        return new Value(ValueType.I64, data);
    }

    /**
     * Creates a new F32 {@link Value} from a raw long value (lower 32 bits interpreted as float bits).
     *
     * @param data the raw long value containing float bits.
     * @return a new {@link Value} of type F32.
     */
    public static Value f32(long data) {
        return new Value(ValueType.F32, data);
    }

    /**
     * Creates a new F64 {@link Value} from a raw long value (interpreted as double bits).
     *
     * @param data the raw long value containing double bits.
     * @return a new {@link Value} of type F64.
     */
    public static Value f64(long data) {
        return new Value(ValueType.F64, data);
    }

    /**
     * Creates a new ExternRef {@link Value} from a raw long value representing the reference.
     * A value of {@link #REF_NULL_VALUE} usually indicates a null reference.
     *
     * @param data the raw long value representing the external reference.
     * @return a new {@link Value} of type ExternRef.
     */
    public static Value externRef(long data) {
        return new Value(ValueType.ExternRef, data);
    }

    /**
     * Creates a new FuncRef {@link Value} from a raw long value representing the function index.
     * A value of {@link #REF_NULL_VALUE} usually indicates a null reference.
     *
     * @param data the raw long value representing the function reference (index).
     * @return a new {@link Value} of type FuncRef.
     */
    public static Value funcRef(long data) {
        return new Value(ValueType.FuncRef, data);
    }

    /**
     * Constructs a new {@link Value} with the specified type and raw data.
     * Use the static factory methods (e.g., {@link #i32(int)}, {@link #fromFloat(float)}) for clearer creation.
     *
     * @param type the {@link ValueType} (must not be {@code null}).
     * @param value the raw 64-bit long representation of the value.
     */
    public Value(ValueType type, long value) {
        this.type = requireNonNull(type, "type");
        data = value;
    }

    /**
     * Packs an array of 64-bit longs (representing v128 lanes) into a byte array.
     * Each long is serialized into 8 bytes (little-endian).
     *
     * @param values the array of longs (v128 lanes).
     * @return the packed byte array.
     */
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

    /**
     * Unpacks a byte array (little-endian encoded longs) back into an array of 64-bit longs.
     * The byte array length must be a multiple of 8.
     *
     * @param bytes the packed byte array.
     * @return the array of longs (v128 lanes).
     */
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

    /**
     * Extracts 16-bit lanes from an array of 64-bit longs (representing v128 values).
     * Each 64-bit long yields four 16-bit integers.
     *
     * @param values the array of longs (v128 lanes).
     * @return an array of 16-bit integers extracted from the longs.
     */
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

    /**
     * Extracts 32-bit lanes from an array of 64-bit longs (representing v128 values).
     * Each 64-bit long yields two 32-bit longs (effectively integers).
     *
     * @param values the array of longs (v128 lanes).
     * @return an array of 32-bit longs extracted from the input longs.
     */
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

    /**
     * Extracts 32-bit float lanes from an array of 64-bit longs (representing v128 values).
     * Each 64-bit long yields two 32-bit floats.
     *
     * @param values the array of longs (v128 lanes).
     * @return an array of floats extracted from the longs.
     */
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

    /**
     * Extracts 64-bit double lanes from an array of 64-bit longs (representing v128 values).
     * Each 64-bit long directly corresponds to one double.
     *
     * @param values the array of longs (v128 lanes).
     * @return an array of doubles extracted from the longs.
     */
    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static double[] vecToF64(long[] values) {
        var result = new double[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.longBitsToDouble(values[i]);
        }
        return result;
    }

    /**
     * Packs multiple arrays of 8-bit longs (simulating v128 lanes) into a single array of 64-bit longs.
     * Each input array is expected to have 16 elements (representing two v128 values).
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 16 8-bit lanes.
     * @return the packed array of 64-bit longs.
     */
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

    /**
     * Packs multiple arrays of 16-bit longs (simulating v128 lanes) into a single array of 64-bit longs.
     * Each input array is expected to have 8 elements (representing two v128 values).
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 8 16-bit lanes.
     * @return the packed array of 64-bit longs.
     */
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

    /**
     * Packs multiple arrays of 32-bit longs (simulating v128 lanes) into a single array of 64-bit longs.
     * Each input array is expected to have 4 elements (representing two v128 values).
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 4 32-bit lanes.
     * @return the packed array of 64-bit longs.
     */
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

    /**
     * Packs multiple arrays of 64-bit longs (simulating v128 lanes) into a single array of 64-bit longs.
     * Each input array is expected to have 2 elements (representing two v128 values).
     * This effectively concatenates the input arrays.
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 2 64-bit lanes.
     * @return the packed array of 64-bit longs.
     */
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

    /**
     * Packs multiple arrays of 32-bit floats (simulating v128 lanes, stored as longs) into a single array of 64-bit longs.
     * Each input array is expected to have 4 elements (representing two v128 values).
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 4 32-bit float lanes.
     * @return the packed array of 64-bit longs.
     */
    public static long[] f32ToVec(long[]... vec) {
        return i32ToVec(vec);
    }

    /**
     * Packs multiple arrays of 64-bit doubles (simulating v128 lanes, stored as longs) into a single array of 64-bit longs.
     * Each input array is expected to have 2 elements (representing two v128 values).
     * This effectively concatenates the input arrays.
     * This seems specific to SIMD v128x2 operations.
     *
     * @param vec variable arguments of long arrays, each representing 2 64-bit double lanes.
     * @return the packed array of 64-bit longs.
     */
    public static long[] f64ToVec(long[]... vec) {
        return i64ToVec(vec);
    }

    /**
     * Returns the zero value for a given {@link ValueType}.
     *
     * @param valueType the type for which to get the zero value.
     * @return the raw long representation of the zero value (0 for numeric types, {@link #REF_NULL_VALUE} for reference types).
     * @throws IllegalArgumentException if the value type is invalid or unsupported.
     */
    public static long zero(ValueType valueType) {
        switch (valueType) {
            case I32:
            case F32:
            case I64:
            case F64:
                return 0L;
            case ExnRef:
            case FuncRef:
            case ExternRef:
                return REF_NULL_VALUE;
            default:
                throw new IllegalArgumentException(
                        "Can't create a zero value for type " + valueType);
        }
    }

    /**
     * Returns a string representation of this value, including its type and data.
     * The data is formatted appropriately based on the type (e.g., float, double, hex for references).
     *
     * @return a string representation of the value.
     */
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
            case V128:
                return data + "@v128";
            case FuncRef:
                return "func[" + (int) data + "]";
            case ExternRef:
                return "ext[" + (int) data + "]";
            default:
                throw new AssertionError("Unhandled type: " + type);
        }
    }

    /**
     * Compares this value to another object for equality.
     * Two values are equal if they have the same {@link ValueType} and the same raw data.
     *
     * @param v the object to compare against.
     * @return {@code true} if the object is a {@code Value} with the same type and data, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this value.
     * The hash code is based on the {@link ValueType} and the raw data.
     *
     * @return the hash code.
     */
    @Override
    public final int hashCode() {
        return Objects.hash(type.id(), data);
    }
}
