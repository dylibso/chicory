package com.dylibso.chicory.wasm.types;

import java.util.List;

/**
 * A passive element.
 * A passive element can be copied into a table using the {@code table.init} instruction.
 */
public final class PassiveElement extends Element {

    /**
     * Construct a new instance.
     *
     * @param type the type of the element values (must not be {@code null})
     * @param initializers the list of instruction lists which are used to initialize each element in the range (must not be {@code null})
     */
    public PassiveElement(ValType type, List<List<Instruction>> initializers) {
        super(type, initializers);
    }
}
