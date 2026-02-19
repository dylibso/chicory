package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import java.util.List;
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
    public static ValType AnyRef = new ValType(ID.AnyRef);
    public static ValType EqRef = new ValType(ID.EqRef);
    public static ValType I31Ref = new ValType(ID.i31);
    public static ValType StructRef = new ValType(ID.StructRef);
    public static ValType ArrayRef = new ValType(ID.ArrayRef);
    public static ValType NoneRef = new ValType(ID.NoneRef);
    public static ValType NoFuncRef = new ValType(ID.NoFuncRef);
    public static ValType NoExternRef = new ValType(ID.NoExternRef);

    public static ValType RefBot = new ValType(ValType.ID.Ref, ValType.TypeIdxCode.BOT.code());

    private final long id;

    // defined function type. This is not representable in the binary or textual representation
    // of WASM. This is instead used after substitution to represent closed ValType.
    // This is useful when validating import function values.
    private int resolvedFunctionTypeHash;
    private final int resolvedFunctionTypeId;

    private ValType(int opcode) {
        this(opcode, NULL_TYPEIDX, -1);
    }

    private ValType(int opcode, int typeIdx) {
        this(opcode, typeIdx, -1);
    }

    private ValType(int opcode, int typeIdx, int resolvedFunctionTypeId) {
        this(opcode, typeIdx, resolvedFunctionTypeId, -1);
    }

    private ValType(
            int opcode, int typeIdx, int resolvedFunctionTypeId, int resolvedFunctionTypeHash) {
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
        } else if (opcode == ID.AnyRef) {
            typeIdx = TypeIdxCode.ANY.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.EqRef) {
            typeIdx = TypeIdxCode.EQ.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.i31) {
            typeIdx = TypeIdxCode.I31.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.StructRef) {
            typeIdx = TypeIdxCode.STRUCT.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.ArrayRef) {
            typeIdx = TypeIdxCode.ARRAY.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.NoneRef) {
            typeIdx = TypeIdxCode.NONE.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.NoExternRef) {
            typeIdx = TypeIdxCode.NOEXTERN.code();
            opcode = ID.RefNull;
        } else if (opcode == ID.NoFuncRef) {
            typeIdx = TypeIdxCode.NOFUNC.code();
            opcode = ID.RefNull;
        } else if ((opcode == ID.RefNull || opcode == ID.Ref) && typeIdx >= 0) {
            assert resolvedFunctionTypeId >= 0;
        }
        this.resolvedFunctionTypeId = resolvedFunctionTypeId;
        this.resolvedFunctionTypeHash = resolvedFunctionTypeHash;

        this.id = createId(opcode, typeIdx);
    }

    public ValType resolve(TypeSection typeSection) {
        if (resolvedFunctionTypeId >= 0) {
            try {
                resolvedFunctionTypeHash =
                        typeSection.getSubType(resolvedFunctionTypeId).hashCode();
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidException("unknown type: " + resolvedFunctionTypeId);
            }
        }
        return this;
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

    public int resolvedFunctionTypeId() {
        return resolvedFunctionTypeId;
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
            case ID.RefNull:
            case ID.ExnRef:
            case ID.AnyRef:
            case ID.EqRef:
            case ID.i31:
            case ID.StructRef:
            case ID.ArrayRef:
            case ID.FuncRef:
            case ID.ExternRef:
            case ID.NoneRef:
            case ID.NoExternRef:
            case ID.NoFuncRef:
                return true;
            default:
                return false;
        }
    }

    public boolean isReference() {
        return isReference(this.opcode());
    }

    // https://webassembly.github.io/gc/core/binary/types.html#heap-types
    public static boolean isAbsHeapType(int opcode) {
        return (opcode == ID.NoFuncRef
                || opcode == ID.NoExternRef
                || opcode == ID.NoneRef
                || opcode == ID.FuncRef
                || opcode == ID.ExternRef
                // TODO: verify?
                || opcode == ID.ExnRef
                || opcode == ID.AnyRef
                || opcode == ID.EqRef
                || opcode == ID.i31
                || opcode == ID.StructRef
                || opcode == ID.ArrayRef);
    }

    /**
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
     */
    private static boolean isValidOpcode(int opcode) {
        switch (opcode) {
            case ID.RefNull:
            case ID.Ref:
            case ID.ExnRef:
            case ID.AnyRef:
            case ID.EqRef:
            case ID.i31:
            case ID.StructRef:
            case ID.ArrayRef:
            case ID.NoneRef:
            case ID.NoExternRef:
            case ID.NoFuncRef:
            case ID.V128:
            case ID.I32:
            case ID.I64:
            case ID.F32:
            case ID.F64:
            case ID.FuncRef:
            case ID.ExternRef:
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

    /**
     * Check if heap type ht1 is a subtype of heap type ht2.
     * Heap types are represented as typeIdx values (negative for abstract, non-negative for concrete).
     */
    public static boolean heapTypeSubtype(int ht1, int ht2, TypeSection ts) {
        if (ht1 == ht2) {
            return true;
        }
        if (ht1 == TypeIdxCode.BOT.code()) {
            return true;
        }

        // none <: all types in the "any" subtree
        if (ht1 == TypeIdxCode.NONE.code()) {
            return heapTypeSubtype(TypeIdxCode.ANY.code(), ht2, ts)
                    || ht2 == TypeIdxCode.ANY.code()
                    || (ht2 >= 0 && ts != null && isConcreteInAnyHierarchy(ht2, ts));
        }
        // nofunc <: all types in the "func" subtree
        if (ht1 == TypeIdxCode.NOFUNC.code()) {
            return ht2 == TypeIdxCode.FUNC.code()
                    || (ht2 >= 0 && ts != null && isConcreteFunc(ht2, ts));
        }
        // noextern <: extern
        if (ht1 == TypeIdxCode.NOEXTERN.code()) {
            return ht2 == TypeIdxCode.EXTERN.code();
        }

        // i31 <: eq <: any
        if (ht1 == TypeIdxCode.I31.code()) {
            return ht2 == TypeIdxCode.EQ.code() || ht2 == TypeIdxCode.ANY.code();
        }
        // struct <: eq <: any
        if (ht1 == TypeIdxCode.STRUCT.code()) {
            return ht2 == TypeIdxCode.EQ.code() || ht2 == TypeIdxCode.ANY.code();
        }
        // array <: eq <: any
        if (ht1 == TypeIdxCode.ARRAY.code()) {
            return ht2 == TypeIdxCode.EQ.code() || ht2 == TypeIdxCode.ANY.code();
        }
        // eq <: any
        if (ht1 == TypeIdxCode.EQ.code()) {
            return ht2 == TypeIdxCode.ANY.code();
        }

        // Concrete type subtyping
        if (ht1 >= 0) {
            if (ts != null) {
                SubType st1 = ts.getSubType(ht1);
                CompType comp = st1.compType();

                // concrete struct <: struct <: eq <: any
                if (comp.structType() != null) {
                    if (ht2 == TypeIdxCode.STRUCT.code()
                            || ht2 == TypeIdxCode.EQ.code()
                            || ht2 == TypeIdxCode.ANY.code()) {
                        return true;
                    }
                }
                // concrete array <: array <: eq <: any
                if (comp.arrayType() != null) {
                    if (ht2 == TypeIdxCode.ARRAY.code()
                            || ht2 == TypeIdxCode.EQ.code()
                            || ht2 == TypeIdxCode.ANY.code()) {
                        return true;
                    }
                }
                // concrete func <: func
                if (comp.funcType() != null) {
                    if (ht2 == TypeIdxCode.FUNC.code()) {
                        return true;
                    }
                }

                // Check declared supertypes (transitively)
                int[] supers = st1.typeIdx();
                for (int sup : supers) {
                    if (heapTypeSubtype(sup, ht2, ts)) {
                        return true;
                    }
                }

                // Structural equivalence: types in different rec groups
                // with identical canonical structure are considered equal
                if (ht2 >= 0 && ts.canonicallyEquivalent(ht1, ht2)) {
                    return true;
                }
            } else {
                // Without TypeSection, assume concrete types are subtypes of FUNC
                // (backwards compatibility for pre-GC code)
                if (ht2 == TypeIdxCode.FUNC.code()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isConcreteInAnyHierarchy(int typeIdx, TypeSection ts) {
        SubType st = ts.getSubType(typeIdx);
        CompType comp = st.compType();
        return comp.structType() != null || comp.arrayType() != null;
    }

    private static boolean isConcreteFunc(int typeIdx, TypeSection ts) {
        SubType st = ts.getSubType(typeIdx);
        return st.compType().funcType() != null;
    }

    public static boolean matchesRef(ValType t1, ValType t2) {
        return matchesRef(t1, t2, null);
    }

    public static boolean matchesRef(ValType t1, ValType t2, TypeSection ts) {
        if (!matchesNull(t1.isNullable(), t2.isNullable())) {
            return false;
        }

        int ht1 = t1.typeIdx();
        int ht2 = t2.typeIdx();

        if (ht1 == ht2) {
            return true;
        }

        return heapTypeSubtype(ht1, ht2, ts);
    }

    public static boolean matches(ValType t1, ValType t2) {
        return matches(t1, t2, null);
    }

    public static boolean matches(ValType t1, ValType t2, TypeSection ts) {
        if (t1.isReference() && t2.isReference()) {
            return matchesRef(t1, t2, ts);
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

    public ValType withNullability(boolean nullable) {
        int targetOpcode = nullable ? ID.RefNull : ID.Ref;
        if (opcode() == targetOpcode) {
            return this;
        }
        return new ValType(targetOpcode, typeIdx());
    }

    public static boolean isAbsHeapTypeIdx(int typeIdx) {
        return typeIdx < 0 && typeIdx != TypeIdxCode.BOT.code();
    }

    @Override
    public int hashCode() {
        if (resolvedFunctionTypeHash != -1) {
            return resolvedFunctionTypeHash;
        }
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ValType)) {
            return false;
        }
        ValType that = (ValType) other;

        // verify reference identity
        if (this == that) {
            return true;
        }

        // For types without resolvedFunctionType, compare by id
        if (this.resolvedFunctionTypeHash == -1 && that.resolvedFunctionTypeHash == -1) {
            return this.id == that.id;
        }

        // If only one has resolvedFunctionType, they're not equal
        if (this.resolvedFunctionTypeHash == -1 || that.resolvedFunctionTypeHash == -1) {
            return false;
        }

        // Both have resolvedFunctionType - need structural comparison
        // Check if opcodes match first
        if (opcode(this.id) != opcode(that.id)) {
            return false;
        }

        // For recursive types: if both have the same typeIdx, they reference the same type
        // definition, so they're equal. This also breaks cycles naturally.
        if (this.typeIdx() >= 0 && this.typeIdx() == that.typeIdx()) {
            return true;
        }

        // For type equivalence: different typeIdx but same structure should be equal
        // Do structural comparison by comparing resolvedFunctionType
        // Cycles are handled by the typeIdx check above - when we recursively compare
        // nested ValTypes with the same typeIdx, we'll return true at the check above
        return this.resolvedFunctionTypeHash == that.resolvedFunctionTypeHash;
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
        NOFUNC(-13), // 0x73
        NOEXTERN(-14), // 0x72
        NONE(-15), // 0x71
        FUNC(-16), // 0x70
        EXTERN(-17), // 0x6F
        ANY(-18), // 0x6E
        EQ(-19), // 0x6D
        I31(-20), // 0x6C
        STRUCT(-21), // 0x6B
        ARRAY(-22), // 0x6A
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
        public static final int ExnRef = 0x69; // -0x17
        public static final int ArrayRef = 0x6A;
        public static final int StructRef = 0x6B;
        public static final int i31 = 0x6C;
        public static final int EqRef = 0x6D;
        public static final int AnyRef = 0x6E;
        public static final int ExternRef = 0x6F;
        public static final int FuncRef = 0x70;
        public static final int NoneRef = 0x71;
        public static final int NoExternRef = 0x72;
        public static final int NoFuncRef = 0x73;
        public static final int V128 = 0x7B;
        public static final int F64 = 0x7C;
        public static final int F32 = 0x7D;
        public static final int I64 = 0x7E;
        public static final int I32 = 0x7F;

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
                    || opcode == AnyRef
                    || opcode == EqRef
                    || opcode == i31
                    || opcode == StructRef
                    || opcode == ArrayRef
                    || opcode == NoneRef
                    || opcode == NoExternRef
                    || opcode == NoFuncRef
                    || opcode == V128
                    || opcode == F64
                    || opcode == F32
                    || opcode == I64
                    || opcode == I32);
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

        @Deprecated(since = "use .build.resolve(typeSection) instead")
        public ValType build(Function<Integer, FunctionType> context) {
            if (!isValidOpcode(opcode)) {
                throw new ChicoryException("Invalid type opcode: " + opcode);
            }

            var resolvedFunctionType = substitute(opcode, typeIdx, context);
            return new ValType(
                    opcode,
                    typeIdx,
                    (ValType.isReference(opcode) && !ValType.isAbsHeapType(opcode) && typeIdx >= 0)
                            ? typeIdx
                            : -1,
                    SubType.builder()
                            .withCompType(
                                    CompType.builder().withFuncType(resolvedFunctionType).build())
                            .build()
                            .hashCode());
        }

        public ValType build() {
            if (!isValidOpcode(opcode)) {
                throw new ChicoryException("Invalid type opcode: " + opcode);
            }

            return new ValType(
                    opcode,
                    typeIdx,
                    (ValType.isReference(opcode) && !ValType.isAbsHeapType(opcode) && typeIdx >= 0)
                            ? typeIdx
                            : -1);
        }

        @Deprecated(since = "use .build.resolve(typeSection) instead")
        public FunctionType substitute(
                int opcode, int typeIdx, Function<Integer, FunctionType> context) {
            if (ValType.isReference(opcode) && typeIdx >= 0) {
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
