package com.dylibso.chicory.wasm.types;

import java.util.List;

/**
 * A declarative element.
 * A declarative element is not available at runtime.
 */
public final class DeclarativeElement extends Element {

    /**
     * Construct a new instance.
     *
     * @param type the type of the element values (must not be {@code null})
     * @param initializers the list of instruction lists which are used to initialize each element in the range (must not be {@code null})
     */
    public DeclarativeElement(ValType type, List<List<Instruction>> initializers) {
        super(type, initializers);
    }
}
