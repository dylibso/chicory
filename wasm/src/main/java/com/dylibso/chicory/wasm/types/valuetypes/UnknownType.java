package com.dylibso.chicory.wasm.types.valuetypes;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.ValueType;

public enum UnknownType implements ValueType {
    UNKNOWN;

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
        return false;
    }

    @Override
    public boolean equals(ValueType other) {
        return other instanceof UnknownType;
    }

    @Override
    public int id() {
        throw new IllegalArgumentException("cannot get id() of UnknownType");
    }

    @Override
    public String shortName() {
        return toString();
    }

    @Override
    public int size() {
        throw new ChicoryException("size on Unknown type");
    }
}
