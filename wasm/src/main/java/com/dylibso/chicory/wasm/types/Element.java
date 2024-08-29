package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

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
     * @return the type of the element values
     */
    public ValueType type() {
        return type;
    }

    /**
     * @return the list of instruction lists which are used to initialize each element in the range
     */
    public Instruction[] initializer(int idx) {
        return initializers.get(idx).toArray(new Instruction[0]);
    }

    /**
     * @return the list of all instructions which are used to initialize each element in the range
     */
    public Instruction[] allInitializers() {
        var result = new ArrayList<Instruction>();
        for (var i : initializers) {
            result.addAll(i);
        }
        return result.toArray(new Instruction[0]);
    }

    /**
     * This value is equal to the number of initializers present.
     *
     * @return the number of elements defined by this section
     */
    public int initializersCount() {
        return initializers.size();
    }
}
