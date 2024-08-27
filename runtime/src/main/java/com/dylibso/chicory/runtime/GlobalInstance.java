package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;

public class GlobalInstance {
    private Value value;
    private Instance instance;
    private MutabilityType mutabilityType;

    public GlobalInstance(Value value, MutabilityType mutabilityType) {
        this.value = value;
        this.mutabilityType = mutabilityType;
    }

    public GlobalInstance(Value value) {
        this.value = value;
        this.mutabilityType = MutabilityType.Const;
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

    public MutabilityType getMutabilityType() {
        return mutabilityType;
    }

    public void setMutabilityType(MutabilityType mutabilityType) {
        this.mutabilityType = mutabilityType;
    }
}
