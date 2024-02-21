package com.dylibso.chicory.runtime;

/**
 * A reference to a WASM function.
 */
public final class FuncRef extends Ref {
    private final Module definingModule;
    private final int index;

    FuncRef(final Module definingModule, final int index) {
        this.definingModule = definingModule;
        this.index = index;
    }

    /**
     * {@return the defining module of this function ref}
     */
    public Module definingModule() {
        return definingModule;
    }

    /**
     * {@return the index of the function within its defining module}
     */
    public int index() {
        return index;
    }

    public String toString() {
        return "func(" + definingModule + "[" + index + "])";
    }
}
