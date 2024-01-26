package com.dylibso.chicory.wasm.types;

import java.util.List;

/**
 * The definition of a global variable in the model of a module.
 */
public final class Global {
    private final ValueType valueType;
    private final MutabilityType mutabilityType;
    private final List<Instruction> init;

    public Global(ValueType valueType, MutabilityType mutabilityType, List<Instruction> init) {
        this.valueType = valueType;
        this.mutabilityType = mutabilityType;
        this.init = List.copyOf(init);
    }

    public MutabilityType mutabilityType() {
        return mutabilityType;
    }

    public ValueType valueType() {
        return valueType;
    }

    public List<Instruction> initInstructions() {
        return init;
    }
}
