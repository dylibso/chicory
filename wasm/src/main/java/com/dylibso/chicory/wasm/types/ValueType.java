package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.ValueType.ID.ExnRef;

import com.dylibso.chicory.wasm.MalformedException;
import java.util.List;

/**
 * Represents the possible value types in WebAssembly.
 * Includes numeric types (I32, I64, F32, F64), vector types (V128),
 * and reference types (FuncRef, ExternRef, ExnRef).
 */
public enum ValueType {
    /** Represents an unknown or invalid value type. */
    UNKNOWN(-1),
    /** 64-bit floating-point number (double). */
    F64(ID.F64),
    /** 32-bit floating-point number (float). */
    F32(ID.F32),
    /** 64-bit integer (long). */
    I64(ID.I64),
    /** 32-bit integer (int). */
    I32(ID.I32),
    /** 128-bit vector type (SIMD). */
    V128(ID.V128),
    /** Reference to a function. */
    FuncRef(ID.FuncRef),
    /** Reference to an exception (part of the Exception Handling proposal). */
    ExnRef(ID.ExnRef),
    /** Reference to an external host value. */
    ExternRef(ID.ExternRef);

    private final int id;

    ValueType(int id) {
        this.id = id;
    }

    /**
     * Returns the numerical identifier (byte code) for this value type.
     *
     * @return the numerical identifier.
     */
    public int id() {
        return id;
    }

    /**
     * Returns the size of this type in bytes when stored in memory.
     *
     * @return the size in bytes (4, 8, or 16).
     * @throws IllegalStateException if the type cannot be stored directly in memory (e.g., reference types).
     */
    public int size() {
        switch (this) {
            case F64:
            case I64:
                return 8;
            case F32:
            case I32:
                return 4;
            case V128:
                return 16;
            default:
                throw new IllegalStateException("Type does not have size");
        }
    }

    /**
     * Checks if this type is a numeric type (I32, I64, F32, F64).
     *
     * @return {@code true} if the type is numeric, {@code false} otherwise.
     */
    public boolean isNumeric() {
        switch (this) {
            case F64:
            case F32:
            case I64:
            case I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if this type is an integer type (I32, I64).
     *
     * @return {@code true} if the type is an integer type, {@code false} otherwise.
     */
    public boolean isInteger() {
        switch (this) {
            case I64:
            case I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if this type is a floating-point type (F32, F64).
     *
     * @return {@code true} if the type is a floating-point type, {@code false} otherwise.
     */
    public boolean isFloatingPoint() {
        switch (this) {
            case F64:
            case F32:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if this type is a reference type (FuncRef, ExternRef, ExnRef).
     *
     * @return {@code true} if the type is a reference type, {@code false} otherwise.
     */
    public boolean isReference() {
        switch (this) {
            case FuncRef:
            case ExnRef:
            case ExternRef:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given numeric ID corresponds to a valid WebAssembly value type.
     *
     * @param typeId the numeric ID to check.
     * @return {@code true} if the ID represents a valid {@link ValueType}, {@code false} otherwise.
     */
    public static boolean isValid(int typeId) {
        switch (typeId) {
            case ID.ExternRef:
            case ID.ExnRef:
            case ID.FuncRef:
            case ID.V128:
            case ID.I32:
            case ID.I64:
            case ID.F32:
            case ID.F64:
                return true;
            default:
                return false;
        }
    }

    /**
     * Retrieves the {@code ValueType} enum constant corresponding to the given numerical ID.
     *
     * @param id the numerical ID of the value type.
     * @return the corresponding {@link ValueType} enum constant.
     * @throws IllegalArgumentException if the ID does not correspond to a valid value type.
     */
    public static ValueType forId(int id) {
        switch (id) {
            case ID.F64:
                return F64;
            case ID.F32:
                return F32;
            case ID.I64:
                return I64;
            case ID.I32:
                return I32;
            case ID.V128:
                return V128;
            case ID.FuncRef:
                return FuncRef;
            case ID.ExnRef:
                return ExnRef;
            case ID.ExternRef:
                return ExternRef;
            default:
                throw new IllegalArgumentException("Invalid value type " + id);
        }
    }

    /**
     * Retrieves the reference-typed {@code ValueType} corresponding to the given numerical ID.
     *
     * @param id the numerical ID of the reference type (e.g., 0x70 for FuncRef).
     * @return the corresponding reference {@link ValueType} enum constant (FuncRef, ExternRef, or ExnRef).
     * @throws MalformedException if the ID does not correspond to a valid reference type.
     */
    public static ValueType refTypeForId(int id) {
        switch (id) {
            case ID.FuncRef:
                return FuncRef;
            case ID.ExternRef:
                return ExternRef;
            case ID.ExnRef:
                return ExnRef;
            default:
                throw new MalformedException("malformed reference type " + id);
        }
    }

    /**
     * Calculates the size of a list of value types in terms of stack slots.
     * V128 counts as 2 slots, other types count as 1.
     *
     * @param args the list of {@link ValueType}s.
     * @return the total number of stack slots occupied by these types.
     */
    public static int sizeOf(List<ValueType> args) {
        int total = 0;
        for (var a : args) {
            if (a == ValueType.V128) {
                total += 2;
            } else {
                total += 1;
            }
        }
        return total;
    }

    /**
     * A separate holder class for ID constants.
     * This is necessary because enum constants are initialized before normal fields, so any reference to an ID constant
     * in the same class would be considered an invalid forward reference.
     */
    static final class ID {
        private ID() {}

        static final int ExternRef = 0x6f;
        // From the Exception Handling proposal
        static final int ExnRef = 0x69; // -0x17
        static final int FuncRef = 0x70;
        static final int V128 = 0x7b;
        static final int F64 = 0x7c;
        static final int F32 = 0x7d;
        static final int I64 = 0x7e;
        static final int I32 = 0x7f;
    }
}
