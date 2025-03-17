package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;
import java.util.List;
import java.util.Objects;

/**
 * The possible WASM value types.
 */
public final class ValueType {
    private static final int EMPTY_OPERAND = 0;
    private static final long OPCODE_MASK = 0x0000FFFFL;
    private static final long OPERAND_MASK = 0xFFFF0000L;
    private static final long OPERAND_SHIFT = 32;

    public static ValueType UNKNOWN = new ValueType(ValueTypeOpCode.UNKNOWN);
    public static ValueType F64 = new ValueType(ValueTypeOpCode.F64);

    public static ValueType F32 = new ValueType(ValueTypeOpCode.F32);
    public static ValueType I64 = new ValueType(ValueTypeOpCode.I64);

    public static ValueType I32 = new ValueType(ValueTypeOpCode.I32);

    public static ValueType V128 = new ValueType(ValueTypeOpCode.V128);
    public static ValueType FuncRef = new ValueType(ValueTypeOpCode.FuncRef);
    public static ValueType ExternRef = new ValueType(ValueTypeOpCode.ExternRef);

    private final ValueTypeOpCode opcode;

    // some value types have an argument, conveniently this fits in an int
    private final int operand;

    ValueType(ValueTypeOpCode opcode) {
        this(opcode, EMPTY_OPERAND);
    }

    ValueType(ValueTypeOpCode opcode, int operand) {
        this.opcode = opcode;
        this.operand = operand;
    }

    /**
     * @return the numerical identifier for this type. Conveniently, all value types we want to represent
     *     for now (constructor + arguments) can fit inside a Java long.
     *
     *     We store as operand in the MSB and the opcode in the LSB.
     */
    public long id() {
        return ((long) operand << OPERAND_SHIFT) | opcode.opcode();
    }

    public ValueTypeOpCode opcode() {
        return opcode;
    }

    /**
     * @return the size of this type in memory
     *
     * @throws IllegalStateException if the type cannot be stored in memory
     */
    public int size() {
        switch (this.opcode.opcode()) {
            case ValueTypeOpCode.ID.F64:
            case ValueTypeOpCode.ID.I64:
                return 8;
            case ValueTypeOpCode.ID.F32:
            case ValueTypeOpCode.ID.I32:
                return 4;
            case ValueTypeOpCode.ID.V128:
                return 16;
            default:
                throw new IllegalStateException("Type does not have size");
        }
    }

    /**
     * @return {@code true} if the type is a numeric type, or {@code false} otherwise
     */
    public boolean isNumeric() {
        switch (this.opcode.opcode()) {
            case ValueTypeOpCode.ID.F64:
            case ValueTypeOpCode.ID.F32:
            case ValueTypeOpCode.ID.I64:
            case ValueTypeOpCode.ID.I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is an integer type, or {@code false} otherwise
     */
    public boolean isInteger() {
        switch (this.opcode.opcode()) {
            case ValueTypeOpCode.ID.I64:
            case ValueTypeOpCode.ID.I32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is a floating-point type, or {@code false} otherwise
     */
    public boolean isFloatingPoint() {
        switch (this.opcode.opcode()) {
            case ValueTypeOpCode.ID.F64:
            case ValueTypeOpCode.ID.F32:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return {@code true} if the type is a reference type, or {@code false} otherwise
     */
    public boolean isReference() {
        switch (this.opcode.opcode()) {
            case ValueTypeOpCode.ID.FuncRef:
            case ValueTypeOpCode.ID.ExternRef:
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
            case ValueTypeOpCode.ID.F64:
            case ValueTypeOpCode.ID.ExternRef:
            case ValueTypeOpCode.ID.FuncRef:
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
        int operand = (int) ((id & OPERAND_MASK) >> OPERAND_SHIFT);
        assert operand == 0;
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
            case ValueTypeOpCode.ID.FuncRef:
                return FuncRef;
            case ValueTypeOpCode.ID.ExternRef:
                return ExternRef;
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
            case ValueTypeOpCode.ID.FuncRef:
                return FuncRef;
            case ValueTypeOpCode.ID.ExternRef:
                return ExternRef;
            default:
                throw new MalformedException("malformed reference type " + id);
        }
    }

    public static int sizeOf(List<ValueType> args) {
        int total = 0;
        for (var a : args) {
            if (a.opcode == ValueTypeOpCode.V128) {
                total += 2;
            } else {
                total += 1;
            }
        }
        return total;
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
        return opcode.name();
    }

    /**
     * a string representation of [ValueType] that follows JVM's naming conventions
     */
    public String name() {
        return opcode.name();
    }
}
