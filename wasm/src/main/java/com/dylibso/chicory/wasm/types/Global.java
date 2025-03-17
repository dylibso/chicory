package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Global)) {
            return false;
        }
        Global global = (Global) o;
        return valueType.equals(global.valueType)
                && mutabilityType == global.mutabilityType
                && Objects.equals(init, global.init);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueType, mutabilityType, init);
    }
}
