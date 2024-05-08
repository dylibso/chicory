package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;

public class GlobalInstance {
    private Value value;
    private Instance instance;

    public GlobalInstance(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }
}
