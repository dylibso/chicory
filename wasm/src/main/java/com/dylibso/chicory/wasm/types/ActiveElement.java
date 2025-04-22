package com.dylibso.chicory.wasm.types;

import java.util.List;

/**
 * An active element.
 * An active element copies its elements into a table during initialization of that table.
 */
public final class ActiveElement extends Element {
    private final int tableIndex;
    private final List<Instruction> offset;

    /**
     * Construct a new instance.
     *
     * @param type the type of the element values (must not be {@code null})
     * @param initializers the list of instruction lists which are used to initialize each element in the range (must not be {@code null})
     * @param tableIndex the index of the table which is to be initialized
     * @param offset the list of instructions which give the offset into the table (must not be {@code null})
     */
    public ActiveElement(
            ValueType type,
            List<List<Instruction>> initializers,
            int tableIndex,
            List<Instruction> offset) {
        super(type, initializers);
        this.tableIndex = tableIndex;
        this.offset = List.copyOf(offset);
    }

    /**
     * Returns the index of the table that this active element segment initializes.
     *
     * @return the target table index.
     */
    public int tableIndex() {
        return tableIndex;
    }

    /**
     * Returns the list of instructions that compute the starting offset within the target table
     * where the elements should be placed. This is typically a single `i32.const` instruction.
     *
     * @return an unmodifiable list of offset instructions.
     */
    public List<Instruction> offset() {
        return offset;
    }
}
