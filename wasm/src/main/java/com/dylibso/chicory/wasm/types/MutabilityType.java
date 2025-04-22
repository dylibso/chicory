package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.MalformedException;

/**
 * The kind of mutability for a global variable.
 */
public enum MutabilityType {
    /** The global is immutable (read-only). */
    Const(ID.Const),
    /** The global is mutable (read-write). */
    Var(ID.Var);

    private final int id;

    MutabilityType(int id) {
        this.id = id;
    }

    /**
     * Returns the numerical identifier (0x00 for Const, 0x01 for Var) for this mutability type.
     *
     * @return the numerical identifier for this type.
     */
    public int id() {
        return id;
    }

    /**
     * Get the mutability type by its ID.
     *
     * @param id the ID of the mutability type (0 for constant, 1 for variable)
     * @return the {@code MutabilityType} for the given ID value
     * @throws IllegalArgumentException if the ID is invalid
     */
    public static MutabilityType forId(int id) {
        switch (id) {
            case ID.Const:
                return Const;
            case ID.Var:
                return Var;
            default:
                throw new MalformedException("Global malformed mutability");
        }
    }

    static final class ID {
        static final int Const = 0x00;
        static final int Var = 0x01;

        private ID() {}
    }
}
