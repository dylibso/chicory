package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

/**
 * Represents the body of a WebAssembly function, including its local variable declarations
 * and the sequence of instructions.
 */
public final class FunctionBody {
    private final List<ValueType> locals;
    private final List<AnnotatedInstruction> instructions;

    /**
     * Constructs a new FunctionBody.
     *
     * @param locals an unmodifiable list of {@link ValueType}s representing the local variables (excluding parameters).
     * @param instructions an unmodifiable list of {@link AnnotatedInstruction}s representing the function's code.
     */
    public FunctionBody(List<ValueType> locals, List<AnnotatedInstruction> instructions) {
        this.locals = List.copyOf(locals);
        this.instructions = List.copyOf(instructions);
    }

    /**
     * Returns the list of types for the local variables declared in this function body.
     * This list does *not* include the function parameters.
     *
     * @return an unmodifiable {@link List} of {@link ValueType}s for the locals.
     */
    public List<ValueType> localTypes() {
        return locals;
    }

    /**
     * Returns the sequence of instructions that make up this function body.
     * These instructions include metadata added during parsing (see {@link AnnotatedInstruction}).
     *
     * @return an unmodifiable {@link List} of {@link AnnotatedInstruction}s.
     */
    public List<AnnotatedInstruction> instructions() {
        return instructions;
    }

    /**
     * Compares this function body to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code FunctionBody} with the same locals and instructions, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof FunctionBody)) {
            return false;
        }
        FunctionBody that = (FunctionBody) o;
        return Objects.equals(locals, that.locals)
                && Objects.equals(instructions, that.instructions);
    }

    /**
     * Computes the hash code for this function body.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(locals, instructions);
    }
}
