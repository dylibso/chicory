package com.dylibso.chicory.wasm.types;

public interface Type {

    public ValueType[] getParams();

    public ValueType[] getReturns();
}
