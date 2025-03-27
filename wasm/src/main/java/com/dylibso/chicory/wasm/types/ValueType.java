package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The possible WASM value types.
 */
public final class ValueType {
    private static final int EMPTY_OPERAND = 0;
    private static final long OPCODE_MASK = 0x0000FFFFL;
    private static final long OPERAND_SHIFT = 32;

    public static ValueType BOT = new ValueType(ValueTypeOpCode.BOT);
    public static ValueType F64 = new ValueType(ValueTypeOpCode.F64);

    public static ValueType F32 = new ValueType(ValueTypeOpCode.F32);
    public static ValueType I64 = new ValueType(ValueTypeOpCode.I64);

    public static ValueType I32 = new ValueType(ValueTypeOpCode.I32);

    public static ValueType V128 = new ValueType(ValueTypeOpCode.V128);
    public static ValueType FuncRef = new ValueType(ValueTypeOpCode.FuncRef);
    public static ValueType ExternRef = new ValueType(ValueTypeOpCode.ExternRef);

    private final Variant opcode;

    // some value types have an argument, conveniently this fits in an int
    private final int operand;

    public ValueType(ValueTypeOpCode opcode) {
        this(opcode, EMPTY_OPERAND);
    }

    public ValueType(ValueTypeOpCode opcode, int operand) {
        Variant var = Variant.ofValueTypeOpCode(opcode);

        // handle operand
        switch (opcode) {
            case FuncRef:
                assert operand == EMPTY_OPERAND;
                operand = OperandCode.FUNC.code();
                break;
            case ExternRef:
                assert operand == EMPTY_OPERAND;
                operand = OperandCode.EXTERN.code();
                break;
            case RefNull:
            case Ref:
                assert operand == OperandCode.FUNC.code()
                        || operand == OperandCode.EXTERN.code()
                        || operand == OperandCode.BOT.code()
                        || operand >= 0;
                break;
            default:
                assert operand == EMPTY_OPERAND;
        }
        this.opcode = var;
        this.operand = operand;
    }

    /**
     * @return the numerical identifier for this type. Conveniently, all value types we want to represent
     *     for now (constructor + arguments) can fit inside a Java long.
     *
     *     We store as operand in the MSB and the opcode in the LSB.
     */
    public long id() {
        return ((long) operand) << OPERAND_SHIFT | opcode.id();
    }

    public Variant opcode() {
        return opcode;
    }

    /**
     * @return the size of this type in memory
     *
     * @throws IllegalStateException if the type cannot be stored in memory
     */
    public int size() {
        switch (this.opcode) {
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
        switch (this.opcode) {
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
        switch (this.opcode) {
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
        switch (this.opcode) {
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
        switch (this.opcode) {
            case Ref:
            case RefNull:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
     */
    public static boolean isValid(long id) {
        int opcode = (int) (id & OPCODE_MASK);
        switch (opcode) {
            case ValueTypeOpCode.ID.F64:
            case ValueTypeOpCode.ID.Ref:
            case ValueTypeOpCode.ID.RefNull:
            case ValueTypeOpCode.ID.V128:
            case ValueTypeOpCode.ID.I32:
            case ValueTypeOpCode.ID.I64:
            case ValueTypeOpCode.ID.F32:
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
    public static ValueType forId(long id) {
        int opcode = (int) (id & OPCODE_MASK);
        int operand = (int) (id >> OPERAND_SHIFT);
        switch (opcode) {
            case ValueTypeOpCode.ID.F64:
                return F64;
            case ValueTypeOpCode.ID.F32:
                return F32;
            case ValueTypeOpCode.ID.I64:
                return I64;
            case ValueTypeOpCode.ID.I32:
                return I32;
            case ValueTypeOpCode.ID.V128:
                return V128;
            case ValueTypeOpCode.ID.Ref:
                return new ValueType(ValueTypeOpCode.Ref, operand);
            case ValueTypeOpCode.ID.RefNull:
                return new ValueType(ValueTypeOpCode.RefNull, operand);
            default:
                throw new IllegalArgumentException("Invalid value type " + id);
        }
    }

    /**
     * @return the reference-typed {@code ValueType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid reference type
     */
    public static ValueType refTypeForId(long id) {
        ValueType res = forId(id);
        switch (res.opcode) {
            case Ref:
            case RefNull:
                return res;
            default:
                throw new MalformedException("malformed reference type " + id);
        }
    }

    public static int sizeOf(List<ValueType> args) {
        int total = 0;
        for (var a : args) {
            if (a.opcode == Variant.V128) {
                total += 2;
            } else {
                total += 1;
            }
        }
        return total;
    }

    private static boolean eq_def(WasmModule context, int typeIdx1, int typeIdx2) {
        var funcType1 = context.typeSection().getType(typeIdx1);
        var funcType2 = context.typeSection().getType(typeIdx2);

        // substitute any type indexes when comparing equality
        if (funcType1.params().size() != funcType2.params().size()
                || funcType1.returns().size() != funcType2.returns().size()) {
            return false;
        }

        ValueType[] types1 =
                Stream.concat(funcType1.params().stream(), funcType1.returns().stream())
                        .toArray(ValueType[]::new);
        ValueType[] types2 =
                Stream.concat(funcType2.params().stream(), funcType2.returns().stream())
                        .toArray(ValueType[]::new);

        for (int i = 0; i < types1.length; i++) {
            var type1 = types1[i];
            var type2 = types2[i];

            if (type1.isReference()
                    && type2.isReference()
                    && type1.operand() >= 0
                    && type2.operand >= 0) {
                // both are defined function types, substitute again!
                if (!eq_def(context, type1.operand(), type2.operand())) {
                    return false;
                }
            } else if (!type1.equals(type2)) {
                return false;
            }
        }

        return true;
    }

    private static boolean matches_null(boolean null1, boolean null2) {
        return null1 == null2 || null2;
    }

    private static boolean matches_heap(WasmModule context, int heapType1, int heapType2) {
        if (heapType1 >= 0 && heapType2 >= 0) {
            return eq_def(context, heapType1, heapType2);
        } else if (heapType1 >= 0 && heapType2 == ValueType.OperandCode.FUNC.code()) {
            return true;
        } else if (heapType1 == ValueType.OperandCode.BOT.code()) {
            return true;
        } else {
            return heapType1 == heapType2;
        }
    }

    public static boolean matches_ref(WasmModule context, ValueType t1, ValueType t2) {
        return matches_heap(context, t1.operand(), t2.operand())
                && matches_null(t1.isNullable(), t2.isNullable());
    }

    public static boolean matches(WasmModule context, ValueType t1, ValueType t2) {
        if (t1.isReference() && t2.isReference()) {
            return matches_ref(context, t1, t2);
        } else if (t1.opcode() == ValueType.Variant.BOT) {
            return true;
        } else {
            return t1.id() == t2.id();
        }
    }

    public boolean isNullable() {
        switch (opcode) {
            case Ref:
                return false;
            case RefNull:
                return true;
            default:
                throw new IllegalArgumentException(
                        "got non-reference type to isNullable(): " + this);
        }
    }

    public int operand() {
        return operand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(opcode, operand);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ValueType)) {
            return false;
        }
        ValueType that = (ValueType) other;
        return this.opcode == that.opcode && this.operand == that.operand;
    }

    public String toString() {
        switch (opcode) {
            case Ref:
            case RefNull:
                return opcode.name() + "[" + operand + "]";
            default:
                return opcode.name();
        }
    }

    /**
     * a string representation of [ValueType] that follows JVM's naming conventions
     */
    public String name() {
        return opcode.name();
    }

    /**
     * basically ValueTypeOpcode, but removes FuncRef and ExternRef, which alias Ref/RefNull
     */
    public static enum Variant {
        BOT(-1),
        F64(ValueTypeOpCode.ID.F64),
        F32(ValueTypeOpCode.ID.F32),
        I64(ValueTypeOpCode.ID.I64),
        I32(ValueTypeOpCode.ID.I32),
        V128(ValueTypeOpCode.ID.V128),
        RefNull(ValueTypeOpCode.ID.RefNull),
        Ref(ValueTypeOpCode.ID.Ref);

        private int id;

        Variant(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static Variant ofValueTypeOpCode(ValueTypeOpCode opcode) {
            switch (opcode) {
                case BOT:
                    return BOT;
                case F64:
                    return F64;
                case F32:
                    return F32;
                case I64:
                    return I64;
                case I32:
                    return I32;
                case V128:
                    return V128;
                case FuncRef:
                    return RefNull;
                case ExternRef:
                    return RefNull;
                case RefNull:
                    return RefNull;
                case Ref:
                    return Ref;
            }

            throw new IllegalArgumentException("could not parse ValueTypeOpCode: " + opcode);
        }
    }

    public enum OperandCode {
        // heap type
        EXTERN(-17), // 0x6F
        FUNC(-16), // 0x70
        BOT(-1);

        private final int code;

        OperandCode(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }
}
