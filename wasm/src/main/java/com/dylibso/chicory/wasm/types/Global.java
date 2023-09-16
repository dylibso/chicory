package com.dylibso.chicory.wasm.types;

public class Global {
    private ValueType valueType;
    private MutabilityType mutabilityType;
    private Instruction[] init;

    public Global(ValueType valueType, MutabilityType mutabilityType, Instruction[] init) {
        this.valueType = valueType;
        this.mutabilityType = mutabilityType;
        this.init = init;
    }

    public MutabilityType getMutabilityType() {
        return mutabilityType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Instruction[] getInit() {
        return init;
    }
}
