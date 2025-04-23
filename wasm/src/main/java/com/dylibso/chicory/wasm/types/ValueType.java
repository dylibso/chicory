package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;
import java.util.List;

/**
 * The possible WASM value types.
 */
@Deprecated(since = "23/05/2025")
public enum ValueType {
    UNKNOWN(-1),
    F64(ID.F64),
    F32(ID.F32),
    I64(ID.I64),
    I32(ID.I32),
    V128(ID.V128),
    FuncRef(ID.FuncRef),
    ExnRef(ID.ExnRef),
    ExternRef(ID.ExternRef);

    private final int id;

    ValueType(int id) {
        this.id = id;
    }

    /**
     * @return the numerical identifier for this type
     */
    public int id() {
        return id;
    }

    /**
     * @return the size of this type in memory
     *
     * @throws IllegalStateException if the type cannot be stored in memory
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
     * @return {@code true} if the type is a numeric type, or {@code false} otherwise
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
     * @return {@code true} if the type is an integer type, or {@code false} otherwise
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
     * @return {@code true} if the type is a floating-point type, or {@code false} otherwise
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
     * @return {@code true} if the type is a reference type, or {@code false} otherwise
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
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
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
     * @return the {@code ValueType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid value type
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

    public ValType toNew() {
        switch (id) {
            case ID.F64:
                return ValType.F64;
            case ID.F32:
                return ValType.F32;
            case ID.I64:
                return ValType.I64;
            case ID.I32:
                return ValType.I32;
            case ID.V128:
                return ValType.V128;
            case ID.FuncRef:
                return ValType.FuncRef;
            case ID.ExnRef:
                return ValType.ExnRef;
            case ID.ExternRef:
                return ValType.ExternRef;
            default:
                throw new IllegalArgumentException("Invalid value type " + id);
        }
    }

    /**
     * @return the reference-typed {@code ValueType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid reference type
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
