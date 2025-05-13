package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.List;
import java.util.stream.Stream;

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

    private final long id;

    public ValType(int opcode) {
        this(opcode, NULL_TYPEIDX);
    }

    public ValType(int opcode, int typeIdx) {
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
        }

        long id = ((long) typeIdx) << TYPEIDX_SHIFT | (opcode & OPCODE_MASK);
        this.id = id;
    }

    private ValType(long id) {
        this.id = id;
    }

    /**
     * @return id of this ValType
     */
    public long id() {
        return this.id;
    }

    public int opcode() {
        return (int) (id & OPCODE_MASK);
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
    public boolean isReference() {
        switch (this.opcode()) {
            case ID.Ref:
            case ID.ExnRef:
            case ID.RefNull:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
     */
    public static boolean isValid(long typeId) {
        ValType res = forId(typeId);
        switch (res.opcode()) {
            case ID.RefNull:
            case ID.Ref:
            case ID.ExnRef:
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
     * @return the {@code ValType} for the given ID value
     * @throws IllegalArgumentException if the ID value does not correspond to a valid value type
     */
    public static ValType forId(long id) {
        return new ValType(id);
    }

    /**
     * @return the reference-typed {@code ValType} for the given ID value
     * @throws IllegalArgumentException if the ID value does not correspond to a valid reference type
     */
    public static ValType refTypeForId(long id) {
        ValType res = forId(id);
        switch (res.opcode()) {
            case ID.RefNull:
            case ID.Ref:
            case ID.ExnRef:
                return res;
            default:
                throw new MalformedException("malformed reference type " + id);
        }
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

    private static boolean matchesResultType(
            WasmModule context, ValType valType1, ValType valType2) {
        if (valType1.equals(valType2) || valType1.equals(BOT)) {
            return true;
        }
        if (valType1.isReference() && valType2.isReference()) {
            return matchesRef(context, valType1, valType2);
        }
        return false;
    }

    private static boolean matchesFunc(
            WasmModule context, FunctionType funcType1, FunctionType funcType2) {
        // substitute any type indexes when comparing equality
        if (funcType1.params().size() != funcType2.params().size()
                || funcType1.returns().size() != funcType2.returns().size()) {
            return false;
        }

        ValType[] types1 =
                Stream.concat(funcType1.params().stream(), funcType1.returns().stream())
                        .toArray(ValType[]::new);
        ValType[] types2 =
                Stream.concat(funcType2.params().stream(), funcType2.returns().stream())
                        .toArray(ValType[]::new);

        for (int i = 0; i < types1.length; i++) {
            var type1 = types1[i];
            var type2 = types2[i];

            if (!matchesResultType(context, type1, type2)) {
                return false;
            }
        }

        return true;
    }

    private static boolean matches_null(boolean null1, boolean null2) {
        return null1 == null2 || null2;
    }

    private static boolean matchesHeap(WasmModule context, Object heapType1, Object heapType2) {
        if (heapType1.equals(TypeIdxCode.BOT.code()) || heapType1.equals(heapType2)) {
            return true;
        } else if ((heapType1 instanceof FunctionType)
                && heapType2.equals(TypeIdxCode.FUNC.code())) {
            return true;
        } else if ((heapType1 instanceof FunctionType) && (heapType2 instanceof FunctionType)) {
            return matchesFunc(context, (FunctionType) heapType1, (FunctionType) heapType2);
        } else if ((heapType1 instanceof Integer) && (Integer) heapType1 >= 0) {
            return matchesHeap(
                    context, context.typeSection().getType((Integer) heapType1), heapType2);
        } else if ((heapType2 instanceof Integer) && (Integer) heapType2 >= 0) {
            return matchesHeap(
                    context, context.typeSection().getType((Integer) heapType2), heapType1);
        }

        return false;
    }

    public static boolean matchesRef(WasmModule context, ValType t1, ValType t2) {
        return matchesHeap(context, t1.typeIdx(), t2.typeIdx())
                && matches_null(t1.isNullable(), t2.isNullable());
    }

    public static boolean matches(WasmModule context, ValType t1, ValType t2) {
        if (t1.isReference() && t2.isReference()) {
            return matchesRef(context, t1, t2);
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

    public int typeIdx() {
        return (int) (id >>> TYPEIDX_SHIFT);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ValType)) {
            return false;
        }
        ValType that = (ValType) other;
        return this.id == that.id;
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
        // From the Exception Handling proposal
        static final int ExnRef = 0x69; // -0x17
        public static final int FuncRef = 0x70;
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

            throw new IllegalArgumentException("got invalid opcode in ValueType.toName: " + opcode);
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
    }
}
