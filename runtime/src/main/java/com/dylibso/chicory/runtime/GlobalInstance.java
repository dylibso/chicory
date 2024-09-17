package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public class GlobalInstance {
    private long value;
    private final ValueType valueType;
    private Instance instance;
    private final MutabilityType mutabilityType;

    public GlobalInstance(Value value, MutabilityType mutabilityType) {
        this.value = value.raw();
        this.valueType = value.type();
        this.mutabilityType = mutabilityType;
    }

    public GlobalInstance(Value value) {
        this.value = value.raw();
        this.valueType = value.type();
        this.mutabilityType = MutabilityType.Const;
    }

    public long getValue() {
        return value;
    }

    public ValueType getType() {
        return valueType;
    }

    public void setValue(Value value) {
        // globals can not be type polimorphic
        assert (value.type() == valueType);
        this.value = value.raw();
    }

    public void setValue(long value) {
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
}
