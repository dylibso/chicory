package com.dylibso.chicory.wasm.types;

import java.util.List;

/**
 * The type of external definition, import, or export.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/types.html#external-types">External Types</a> for
 * reference.
 * See also <a href="https://github.com/WebAssembly/exception-handling/blob/main/proposals/exception-handling/Exceptions.md#external_kind">Exceptions</a>
 * for the history of {@link #TAG}.
 */
public enum ExternalType {
    /** External type for Functions (ID 0). */
    FUNCTION(0x00),
    /** External type for Tables (ID 1). */
    TABLE(0x01),
    /** External type for Memories (ID 2). */
    MEMORY(0x02),
    /** External type for Globals (ID 3). */
    GLOBAL(0x03),
    /** External type for Tags (Exceptions) (ID 4). */
    TAG(0x04);

    private final int id;

    @SuppressWarnings("EnumOrdinal")
    ExternalType(int id) {
        this.id = id;
        assert ordinal() == id;
    }

    /**
     * Returns the numerical identifier for this external type (0x00 to 0x04).
     *
     * @return the numerical identifier for this external kind.
     */
    public int id() {
        return id;
    }

    private static final List<ExternalType> values = List.of(values());

    /**
     * Retrieves the {@code ExternalType} corresponding to the given numerical ID.
     *
     * @param id the numerical ID (0-4).
     * @return the corresponding {@link ExternalType} enum constant.
     * @throws IndexOutOfBoundsException if the ID is invalid.
     */
    public static ExternalType byId(int id) {
        return values.get(id);
    }
}
