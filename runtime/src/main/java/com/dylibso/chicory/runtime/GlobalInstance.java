package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public class GlobalInstance {
    private long[] values;
    private final ValueType valueType;
    private Instance instance;
    private final MutabilityType mutabilityType;

    public GlobalInstance(Value value) {
        this(value, MutabilityType.Const);
    }

    public GlobalInstance(Value value, MutabilityType mutabilityType) {
        this.values = new long[] {value.raw()};
        this.valueType = value.type();
        this.mutabilityType = mutabilityType;
    }

    public GlobalInstance(long[] values, ValueType valueType, MutabilityType mutabilityType) {
        this.values = values.clone();
        this.valueType = valueType;
        this.mutabilityType = mutabilityType;
    }

    public long[] getValues() {
        return values;
    }

    public long getValue() {
        return values[0];
    }

    public ValueType getType() {
        return valueType;
    }

    public void setValue(Value value) {
        // globals can not be type polimorphic
        assert (value.type() == valueType);
        this.values[0] = value.raw();
    }

    public void setValue(long value) {
        this.values[0] = value;
    }

    public void setValues(long[] values) {
        this.values = values;
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
