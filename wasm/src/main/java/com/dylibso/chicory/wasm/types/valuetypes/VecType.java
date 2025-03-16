package com.dylibso.chicory.wasm.types.valuetypes;

import com.dylibso.chicory.wasm.types.ValueType;

public enum VecType implements ValueType {
    V128;

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
        return true;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean equals(ValueType other) {
        return other instanceof VecType;
    }

    @Override
    public int id() {
        return ID.V128;
    }

    @Override
    public String shortName() {
        return toString();
    }

    @Override
    public int size() {
        return 2;
    }
}
