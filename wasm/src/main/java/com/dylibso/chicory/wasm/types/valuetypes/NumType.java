package com.dylibso.chicory.wasm.types.valuetypes;

import com.dylibso.chicory.wasm.types.ValueType;

public enum NumType implements ValueType {
    I32,
    I64,
    F32,
    F64;

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isInteger() {
        return this == I32 || this == I64;
    }

    @Override
    public boolean isFloatingPoint() {
        return this == F32 || this == F64;
    }

    @Override
    public boolean isVec() {
        return false;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean equals(ValueType other) {
        if (!(other instanceof NumType)) {
            return false;
        }

        NumType that = (NumType) other;
        return this == that;
    }

    @Override
    public int id() {
        switch (this) {
            case I32:
                return ID.I32;
            case I64:
                return ID.I64;
            case F32:
                return ID.F32;
            case F64:
                return ID.F64;
            default:
                throw new IllegalArgumentException("Invalid value type " + this);
        }
    }

    @Override
    public String shortName() {
        return toString();
    }

    @Override
    public int size() {
        return 1;
    }
}
