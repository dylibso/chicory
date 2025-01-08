package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public class GlobalInstance {
    private long valueLow;
    private long valueHigh;
    private final ValueType valueType;
    private Instance instance;
    private final MutabilityType mutabilityType;

    public GlobalInstance(Value value) {
        this(value, MutabilityType.Const);
    }

    public GlobalInstance(Value value, MutabilityType mutabilityType) {
        this.valueLow = value.raw();
        this.valueHigh = 0;
        this.valueType = value.type();
        this.mutabilityType = mutabilityType;
    }

    public GlobalInstance(
            long valueLow, long valueHigh, ValueType valueType, MutabilityType mutabilityType) {
        this.valueLow = valueLow;
        this.valueHigh = valueHigh;
        this.valueType = valueType;
        this.mutabilityType = mutabilityType;
    }

    public long getValueLow() {
        return valueLow;
    }

    public long getValueHigh() {
        return valueHigh;
    }

    public long getValue() {
        return valueLow;
    }

    public ValueType getType() {
        return valueType;
    }

    public void setValue(Value value) {
        // globals can not be type polimorphic
        assert (value.type() == valueType);
        this.valueLow = value.raw();
    }

    public void setValue(long value) {
        this.valueLow = value;
    }

    public void setValueLow(long value) {
        this.valueLow = value;
    }

    public void setValueHigh(long value) {
        this.valueHigh = value;
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
