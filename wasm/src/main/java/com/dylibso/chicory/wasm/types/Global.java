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

    /**
     * Constructs a new Global variable definition.
     *
     * @param valueType the {@link ValueType} of the global.
     * @param mutabilityType the {@link MutabilityType} (Const or Var) of the global.
     * @param init an unmodifiable list of {@link Instruction}s representing the initialization expression.
     */
    public Global(ValueType valueType, MutabilityType mutabilityType, List<Instruction> init) {
        this.valueType = valueType;
        this.mutabilityType = mutabilityType;
        this.init = List.copyOf(init);
    }

    /**
     * Returns the mutability type (Const or Var) of this global variable.
     *
     * @return the {@link MutabilityType}.
     */
    public MutabilityType mutabilityType() {
        return mutabilityType;
    }

    /**
     * Returns the value type of this global variable.
     *
     * @return the {@link ValueType}.
     */
    public ValueType valueType() {
        return valueType;
    }

    /**
     * Returns the list of instructions that form the initialization expression for this global.
     * This expression must produce a value of the type specified by {@link #valueType()}.
     *
     * @return an unmodifiable {@link List} of {@link Instruction}s.
     */
    public List<Instruction> initInstructions() {
        return init;
    }

    /**
     * Compares this global definition to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code Global} with the same value type, mutability, and initialization expression, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Global)) {
            return false;
        }
        Global global = (Global) o;
        return valueType == global.valueType
                && mutabilityType == global.mutabilityType
                && Objects.equals(init, global.init);
    }

    /**
     * Computes the hash code for this global definition.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(valueType, mutabilityType, init);
    }
}
