package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

/**
 * An element, used to initialize table ranges.
 */
public abstract class Element {
    private final ValType type;
    private final List<List<Instruction>> initializers;

    /**
     * Construct a new instance.
     *
     * @param type the type of the element values (must not be {@code null})
     * @param initializers the list of instruction lists which are used to initialize each element in the range (must not be {@code null})
     */
    Element(ValType type, List<List<Instruction>> initializers) {
        this.type = requireNonNull(type, "type");
        this.initializers = List.copyOf(initializers);
    }

    /**
     * @return the type of the element values
     */
    public ValType type() {
        return type;
    }

    /**
     * @return the list of instruction lists which are used to initialize each element in the range
     */
    public List<List<Instruction>> initializers() {
        return initializers;
    }

    /**
     * This value is equal to the number of initializers present.
     *
     * @return the number of elements defined by this section
     */
    public int elementCount() {
        return initializers().size();
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(type, initializers);
    }
}
