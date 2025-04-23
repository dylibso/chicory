package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

/**
 * The definition of a global variable in the model of a module.
 */
public final class Global {
    private final ValType valType;
    private final MutabilityType mutabilityType;
    private final List<Instruction> init;

    public Global(ValType valType, MutabilityType mutabilityType, List<Instruction> init) {
        this.valType = valType;
        this.mutabilityType = mutabilityType;
        this.init = List.copyOf(init);
    }

    @Deprecated(since = "23/05/2025", forRemoval = true)
    public Global(ValueType valueType, MutabilityType mutabilityType, List<Instruction> init) {
        this.valType = valueType.toNew();
        this.mutabilityType = mutabilityType;
        this.init = List.copyOf(init);
    }

    public MutabilityType mutabilityType() {
        return mutabilityType;
    }

    public ValType valueType() {
        return valType;
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
        return valType.equals(global.valType)
                && mutabilityType == global.mutabilityType
                && Objects.equals(init, global.init);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valType, mutabilityType, init);
    }
}
