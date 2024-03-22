package com.dylibso.chicory.wasm.types;

/**
 * The possible WASM value types.
 */
public enum ValueType {
    F64(ID.F64),
    F32(ID.F32),
    I64(ID.I64),
    I32(ID.I32),
    V128(ID.V128),
    FuncRef(ID.FuncRef),
    ExternRef(ID.ExternRef),
    VecRef(ID.ExternRef);

    private final int id;

    ValueType(int id) {
        this.id = id;
    }

    /**
     * {@return the numerical identifier for this type}
     */
    public int id() {
        return id;
    }

    /**
     * {@return <code>true</code> if the type can be stored in memory, and thus has a size, or <code>false</code> otherwise}
     */
    public boolean hasSize() {
        switch (this) {
            case F64:
            case F32:
            case I64:
            case I32:
            case V128:
                return true;
            default:
                return false;
        }
    }

    /**
     * {@return the size of this type in memory}
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
     * {@return <code>true</code> if the type is a numeric type, or <code>false</code> otherwise}
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
     * {@return <code>true</code> if the type is an integer type, or <code>false</code> otherwise}
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
     * {@return <code>true</code> if the type is a floating-point type, or <code>false</code> otherwise}
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
     * {@return <code>true</code> if the type is a reference type, or <code>false</code> otherwise}
     */
    public boolean isReference() {
        switch (this) {
            case FuncRef:
            case ExternRef:
                return true;
            default:
                return false;
        }
    }

    /**
     * {@return <code>true</code> if the given type ID is a valid value type ID, or <code>false</code> if it is not}
     */
    public static boolean isValid(final int typeId) {
        switch (typeId) {
            case ID.F64:
            case ID.ExternRef:
            case ID.FuncRef:
            case ID.V128:
            case ID.I32:
            case ID.I64:
            case ID.F32:
                return true;
            default:
                return false;
        }
    }

    /**
     * {@return the <code>ValueType</code> for the given ID value}
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
            case ID.ExternRef:
                return ExternRef;
            default:
                throw new IllegalArgumentException("Invalid value type");
        }
    }

    /**
     * {@return the reference-typed <code>ValueType</code> for the given ID value}
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid reference type
     */
    public static ValueType refTypeForId(int id) {
        switch (id) {
            case ID.FuncRef:
                return FuncRef;
            case ID.ExternRef:
                return ExternRef;
            default:
                throw new IllegalArgumentException("Invalid reference type");
        }
    }

    /**
     * A separate holder class for ID constants.
     * This is necessary because enum constants are initialized before normal fields, so any reference to an ID constant
     * in the same class would be considered an invalid forward reference.
     */
    static final class ID {
        private ID() {}

        static final int VecRef = 0x80;
        static final int ExternRef = 0x6f;
        static final int FuncRef = 0x70;
        static final int V128 = 0x7b;
        static final int F64 = 0x7c;
        static final int F32 = 0x7d;
        static final int I64 = 0x7e;
        static final int I32 = 0x7f;
    }
}
