package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The possible WASM value types.
 */
public final class ValType {
    private static final int NULL_TYPEIDX = 0;
    private static final long OPCODE_MASK = 0xFFFFFFFFL;
    private static final long TYPEIDX_SHIFT = 32;

    public static ValType BOT = new ValType(ID.BOT);
    public static ValType F64 = new ValType(ID.F64);

    public static ValType F32 = new ValType(ID.F32);
    public static ValType I64 = new ValType(ID.I64);

    public static ValType I32 = new ValType(ID.I32);

    public static ValType V128 = new ValType(ID.V128);
    public static ValType FuncRef = new ValType(ID.FuncRef);
    public static ValType ExnRef = new ValType(ID.ExnRef);
    public static ValType ExternRef = new ValType(ID.ExternRef);

    public static ValType RefBot = new ValType(ValType.ID.Ref, ValType.TypeIdxCode.BOT.code());

    private final long id;

    // defined function type. This is not representable in the binary or textual representation
    // of WASM. This is instead used after substitution to represent closed ValType.
    // This is useful when validating import function values.
    private final RecType resolvedRecType;

    private ValType(int opcode) {
        this(opcode, NULL_TYPEIDX, null);
    }

    private ValType(int opcode, int typeIdx) {
        this(opcode, typeIdx, null);
    }

    private ValType(int opcode, int typeIdx, RecType resolvedRecType) {
        // Conveniently, all value types we want to represent can fit inside a Java long.
        // We store the typeIdx (of reference types) in the upper 4 bytes and the opcode in the
        // lower 4 bytes.
        if (opcode == ID.FuncRef) {
            typeIdx = TypeIdxCode.FUNC.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.ExternRef) {
            typeIdx = TypeIdxCode.EXTERN.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.ExnRef) {
            typeIdx = TypeIdxCode.EXN.code();
            opcode = ID.RefNull;
        } else if ((opcode == ID.RefNull || opcode == ID.Ref)
                && typeIdx >= 0
                && !ValType.ID.isAbsHeapType(typeIdx)) {
            Objects.requireNonNull(resolvedRecType);
        }
        this.resolvedRecType = resolvedRecType;

        this.id = createId(opcode, typeIdx);
    }

    private static long createId(int opcode, int typeIdx) {
        return ((long) typeIdx) << TYPEIDX_SHIFT | (opcode & OPCODE_MASK);
    }

    /**
     * @return id of this ValType
     */
    public long id() {
        return this.id;
    }

    private static int opcode(long id) {
        return (int) (id & OPCODE_MASK);
    }

    public int opcode() {
        return opcode(id);
    }

    private static int typeIdx(long id) {
        return (int) (id >>> TYPEIDX_SHIFT);
    }

    public int typeIdx() {
        return typeIdx(id);
    }

    /**
     * @return the size of this type in memory
     * @throws IllegalStateException if the type cannot be stored in memory
     */
    public int size() {
        switch (this.opcode()) {
            case ID.F64:
            case ID.I64:
                return 8;
            case ID.F32:
            case ID.I32:
                return 4;
            case ID.V128:
                return 16;
            default:
                throw new IllegalStateException("Type does not have size");
        }
    }

    /**
     * @return {@code true} if the type is a numeric type, or {@code false} otherwise
     */
    public boolean isNumeric() {
        switch (this.opcode()) {
            case ID.F64:
            case ID.F32:
            case ID.I64:
            case ID.I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is an integer type, or {@code false} otherwise
     */
    public boolean isInteger() {
        switch (this.opcode()) {
            case ID.I64:
            case ID.I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is a floating-point type, or {@code false} otherwise
     */
    public boolean isFloatingPoint() {
        switch (this.opcode()) {
            case ID.F64:
            case ID.F32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is a reference type, or {@code false} otherwise
     */
    private static boolean isReference(int opcode) {
        switch (opcode) {
            case ID.Ref:
            case ID.ExnRef:
            case ID.RefNull:
                return true;
            default:
                return false;
        }
    }

    public boolean isReference() {
        return isReference(this.opcode());
    }

    /**
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
     */
    private static boolean isValidOpcode(int opcode) {
        switch (opcode) {
            case ID.RefNull:
            case ID.Ref:
            case ID.ExnRef:
            case ID.V128:
            case ID.I32:
            case ID.I64:
            case ID.F32:
            case ID.F64:
            case ID.FuncRef:
            case ID.ExternRef:
            case ID.AnyRef:
            case ID.EqRef:
            case ID.i31:
            case ID.StructRef:
            case ID.ArrayRef:
                return true;
            default:
                return false;
        }
    }

    public static boolean isValid(long id) {
        return isValidOpcode(opcode(id));
    }

    public static int sizeOf(List<ValType> args) {
        int total = 0;
        for (var a : args) {
            if (a.opcode() == ID.V128) {
                total += 2;
            } else {
                total += 1;
            }
        }
        return total;
    }

    private static boolean matchesNull(boolean null1, boolean null2) {
        return null1 == null2 || null2;
    }

    public static boolean matchesRef(ValType t1, ValType t2) {
        var matchesNull = matchesNull(t1.isNullable(), t2.isNullable());
        if (!matchesNull) {
            return false;
        }

        if (t1.typeIdx() >= 0 && t2.typeIdx() == TypeIdxCode.FUNC.code()) {
            return true;
        } else if (t1.typeIdx() >= 0 && t2.typeIdx() >= 0) {
            return t1.resolvedRecType.equals(t2.resolvedRecType);
        } else if (t1.typeIdx() == TypeIdxCode.BOT.code()) {
            return true;
        }
        return t1.typeIdx() == t2.typeIdx();
    }

    public static boolean matches(ValType t1, ValType t2) {
        if (t1.isReference() && t2.isReference()) {
            return matchesRef(t1, t2);
        } else if (t1.opcode() == ID.BOT) {
            return true;
        } else {
            return t1.id() == t2.id();
        }
    }

    public boolean isNullable() {
        switch (opcode()) {
            case ID.Ref:
                return false;
            case ID.RefNull:
                return true;
            default:
                throw new IllegalArgumentException(
                        "got non-reference type to isNullable(): " + this);
        }
    }

    @Override
    public int hashCode() {
        if (this.resolvedRecType != null) {
            return resolvedRecType.hashCode();
        }
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ValType)) {
            return false;
        }
        ValType that = (ValType) other;

        if (this.resolvedRecType != null && that.resolvedRecType != null) {
            return opcode(this.id) == opcode(that.id)
                    && this.resolvedRecType.equals(that.resolvedRecType);
        } else if (this.resolvedRecType == null && that.resolvedRecType == null) {
            return this.id == that.id;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        switch (opcode()) {
            case ID.Ref:
            case ID.RefNull:
                return ID.toName(opcode()) + "[" + typeIdx() + "]";
            default:
                return ID.toName(opcode());
        }
    }

    /**
     * a string representation of [ValType] that follows JVM's naming conventions
     */
    public String name() {
        return ID.toName(opcode());
    }

    public enum TypeIdxCode {
        // heap type
        EXTERN(-17), // 0x6F
        FUNC(-16), // 0x70
        EXN(-23), // 0x69
        BOT(-1);

        private final int code;

        TypeIdxCode(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }

    /**
     * A separate holder class for ID constants.
     * This is necessary because enum constants are initialized before normal fields, so any reference to an ID constant
     * in the same class would be considered an invalid forward reference.
     */
    public static final class ID {
        private ID() {}

        public static final int BOT = -1;
        public static final int RefNull = 0x63;
        public static final int Ref = 0x64;
        public static final int ExternRef = 0x6f;
        public static final int AnyRef = 0x6E;
        public static final int EqRef = 0x6D;
        public static final int i31 = 0x6C;
        public static final int StructRef = 0x6B;
        public static final int ArrayRef = 0x6A;
        public static final int ExnRef = 0x69; // -0x17
        public static final int FuncRef = 0x70;
        public static final int NoneRef = 0x71;
        public static final int NoExternRef = 0x72;
        public static final int NoFuncRef = 0x73;
        public static final int V128 = 0x7b;
        public static final int F64 = 0x7c;
        public static final int F32 = 0x7d;
        public static final int I64 = 0x7e;
        public static final int I32 = 0x7f;

        public static String toName(int opcode) {
            switch (opcode) {
                case BOT:
                    return "Bot";
                case RefNull:
                    return "RefNull";
                case Ref:
                    return "Ref";
                case ExnRef:
                    return "ExnRef";
                case V128:
                    return "V128";
                case F64:
                    return "F64";
                case F32:
                    return "F32";
                case I64:
                    return "I64";
                case I32:
                    return "I32";
            }

            throw new IllegalArgumentException("got invalid opcode in ValType.toName: " + opcode);
        }

        public static boolean isValidOpcode(int opcode) {
            return (opcode == RefNull
                    || opcode == Ref
                    || opcode == ExternRef
                    || opcode == FuncRef
                    || opcode == ExnRef
                    || opcode == V128
                    || opcode == F64
                    || opcode == F32
                    || opcode == I64
                    || opcode == I32);
        }

        // https://webassembly.github.io/gc/core/binary/types.html#heap-types
        public static boolean isAbsHeapType(int opcode) {
            return (opcode == NoFuncRef
                    || opcode == NoExternRef
                    || opcode == NoneRef
                    || opcode == FuncRef
                    || opcode == ExternRef
                    // TODO: verify!
                    || opcode == ExnRef
                    || opcode == AnyRef
                    || opcode == EqRef
                    || opcode == i31
                    || opcode == StructRef
                    || opcode == ArrayRef);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int opcode;
        private int typeIdx = NULL_TYPEIDX;

        private Builder() {}

        public Builder withOpcode(int opcode) {
            this.opcode = opcode;
            return this;
        }

        public Builder withTypeIdx(int typeIdx) {
            this.typeIdx = typeIdx;
            return this;
        }

        public Builder fromId(long id) {
            this.opcode = opcode(id);
            this.typeIdx = ValType.typeIdx(id);
            return this;
        }

        public long id() {
            return createId(opcode, typeIdx);
        }

        public int typeIdx() {
            return typeIdx;
        }

        public boolean isReference() {
            return ValType.isReference(opcode);
        }

        public ValType build() {
            return build(
                    (i) -> {
                        throw new ChicoryException("build with empty context tried resolving " + i);
                    });
        }

        public ValType build(Function<Integer, RecType> context) {
            if (!isValidOpcode(opcode)) {
                throw new ChicoryException("Invalid type opcode: " + opcode);
            }

            var resolvedRecType = substitute(opcode, typeIdx, context);
            return new ValType(opcode, typeIdx, resolvedRecType);
        }

        public RecType substitute(int opcode, int typeIdx, Function<Integer, RecType> context) {
            if (ValType.isReference(opcode) && typeIdx >= 0 && !ValType.ID.isAbsHeapType(typeIdx)) {
                // no need to recursively substitute because all ValType are fully resolved
                try {
                    return context.apply(typeIdx);
                } catch (IndexOutOfBoundsException e) {
                    throw new InvalidException("unknown type: " + typeIdx);
                }
            }

            return null;
        }
    }
}
