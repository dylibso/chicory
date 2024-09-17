package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public class GlobalInstance {
    private Value value;
    // TODO: we can remove the boxing also here
    private final ValueType valueType;
    private Instance instance;
    private final MutabilityType mutabilityType;

    public GlobalInstance(Value value, MutabilityType mutabilityType) {
        this.value = value;
        this.valueType = value.type();
        this.mutabilityType = mutabilityType;
    }

    public GlobalInstance(Value value) {
        this.value = value;
        this.valueType = value.type();
        this.mutabilityType = MutabilityType.Const;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public void setValue(long value) {
        this.value = new Value(valueType, value);
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
}
