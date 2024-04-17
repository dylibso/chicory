package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;

public class GlobalInstance {
    private Value value;

    public GlobalInstance(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }
}
