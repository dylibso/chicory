package com.dylibso.chicory.wasm.types.valuetypes;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Objects;

public class RefType implements ValueType {
    private final HeapType heapType;
    private final boolean isNullable;

    public RefType(HeapType heapType) {
        this.heapType = heapType;
        this.isNullable = true;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isFloatingPoint() {
        return false;
    }

    @Override
    public boolean isVec() {
        return false;
    }

    @Override
    public boolean isReference() {
        return true;
    }

    @Override
    public boolean equals(ValueType other) {
        if (!(other instanceof RefType)) {
            return false;
        }

        RefType that = (RefType) other;
        return this.isNullable == that.isNullable && this.heapType.equals(that.heapType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heapType, isNullable);
    }

    @Override
    public String toString() {
        return String.format("RefType[%b, %s]", isNullable, heapType.toString());
    }

    @Override
    public int id() {
        if (heapType instanceof HeapType.Union) {
            HeapType.Union funcUnionType = (HeapType.Union) heapType;
            switch (funcUnionType) {
                case FUNC:
                    return ID.FuncRef;
                case EXTERN:
                    return ID.ExternRef;
                default:
                    throw new IllegalArgumentException("Invalid union type: " + funcUnionType);
            }
        }

        throw new IllegalArgumentException("Invalid heap type: " + heapType);
    }

    @Override
    public String shortName() {
        return heapType.toString();
    }

    @Override
    public int size() {
        return 1;
    }

    public interface HeapType {
        boolean equals(HeapType other);

        // in the future, there will be a Def implementation
        // see https://webassembly.github.io/function-references/core/appendix/algorithm.html

        int hashCode();

        String toString();

        /**
         * Heap types that represent a union of all types of functions.
         */
        enum Union implements HeapType {
            FUNC,
            EXTERN;

            @Override
            public boolean equals(HeapType other) {
                if (!(other instanceof Union)) {
                    return false;
                }

                Union that = (Union) other;
                return this == that;
            }
        }
    }
}
