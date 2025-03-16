package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.types.valuetypes.ID;
import com.dylibso.chicory.wasm.types.valuetypes.NumType;
import com.dylibso.chicory.wasm.types.valuetypes.RefType;
import com.dylibso.chicory.wasm.types.valuetypes.UnknownType;
import com.dylibso.chicory.wasm.types.valuetypes.VecType;
import java.util.List;

/**
 * The possible WASM value types implement this interface.
 *
 * We should use Sealed Classes to make this more explicit.
 */
public interface ValueType {
    ValueType UNKNOWN = UnknownType.UNKNOWN;
    ValueType F64 = NumType.F64;
    ValueType F32 = NumType.F32;
    ValueType I64 = NumType.I64;
    ValueType I32 = NumType.I32;
    ValueType V128 = VecType.V128;
    ValueType FuncRef = new RefType(RefType.HeapType.Union.FUNC);
    ValueType ExternRef = new RefType(RefType.HeapType.Union.EXTERN);

    /**
     * @return the numerical identifier for this type
     */
    int id();

    /**
     * @return abbreviated name, useful for method generation
     */
    String shortName();

    /**
     * @return the size of this type in memory
     *
     * @throws IllegalStateException if the type cannot be stored in memory
     */
    int size();

    /**
     * @return {@code true} if the type is a numeric type, or {@code false} otherwise
     */
    boolean isNumeric();

    /**
     * @return {@code true} if the type is an integer type, or {@code false} otherwise
     */
    boolean isInteger();

    /**
     * @return {@code true} if the type is a floating-point type, or {@code false} otherwise
     */
    boolean isFloatingPoint();

    /**
     * @return {@code true} if the type is a reference type, or {@code false} otherwise
     */
    boolean isReference();

    /**
     * @return {@code true} if the type is a vector type, or {@code false} otherwise
     */
    boolean isVec();

    boolean equals(ValueType other);

    int hashCode();

    String toString();

    /**
     * @return {@code true} if the given type ID is a valid value type ID, or {@code false} if it is not
     */
    static boolean isValid(int typeId) {
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
     * @return the {@code ValueType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid value type
     */
    static ValueType forId(int id) {
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
                throw new IllegalArgumentException("Invalid value type " + id);
        }
    }

    /**
     * @return the reference-typed {@code ValueType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid reference type
     */
    static ValueType refTypeForId(int id) {
        switch (id) {
            case ID.FuncRef:
                return FuncRef;
            case ID.ExternRef:
                return ExternRef;
            default:
                throw new MalformedException("malformed reference type " + id);
        }
    }

    static int sizeOf(List<ValueType> args) {
        int total = 0;
        for (var t : args) {
            total += t.size();
        }
        return total;
    }
}
