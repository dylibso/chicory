package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

/**
 * An element, used to initialize table ranges.
 */
public abstract class Element {
    private final ValueType type;
    private final List<List<Instruction>> initializers;

    /**
     * Construct a new instance.
     *
     * @param type the type of the element values (must not be {@code null})
     * @param initializers the list of instruction lists which are used to initialize each element in the range (must not be {@code null})
     */
    Element(ValueType type, List<List<Instruction>> initializers) {
        this.type = requireNonNull(type, "type");
        this.initializers = List.copyOf(initializers);
    }

    /**
     * Returns the type of the elements defined in this segment (e.g., FuncRef, ExternRef).
     *
     * @return the {@link ValueType} of the elements.
     */
    public ValueType type() {
        return type;
    }

    /**
     * Returns the list of initialization expressions.
     * Each inner list represents the instructions for computing a single element value.
     * For segments using element indices directly (older Wasm versions), this might contain single `ref.func` or similar instructions.
     *
     * @return an unmodifiable list of initialization instruction lists.
     */
    public List<List<Instruction>> initializers() {
        return initializers;
    }

    /**
     * Returns the number of element initializers defined in this segment.
     * This corresponds to the number of elements this segment will initialize.
     *
     * @return the number of element initializers.
     */
    public int elementCount() {
        return initializers().size();
    }

    /**
     * Compares this element segment to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is an {@code Element} with the same type and initializers, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Element)) {
            return false;
        }
        Element element = (Element) o;
        return type == element.type && Objects.equals(initializers, element.initializers);
    }

    /**
     * Computes the hash code for this element segment.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, initializers);
    }
}
