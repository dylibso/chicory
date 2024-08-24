package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.exceptions.MalformedException;

/**
 * The kind of mutability for a global variable.
 */
public enum MutabilityType {
    Const(ID.Const),
    Var(ID.Var);

    private final int id;

    MutabilityType(int id) {
        this.id = id;
    }

    /**
     * @return the numerical identifier for this type
     */
    public int id() {
        return id;
    }

    /**
     * @return the {@code MutabilityType} for the given ID value
     *
     * @throws IllegalArgumentException if the ID value does not correspond to a valid mutability type
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
