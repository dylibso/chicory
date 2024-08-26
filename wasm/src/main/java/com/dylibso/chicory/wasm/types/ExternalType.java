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
    // note: keep in order
    FUNCTION(0x00),
    TABLE(0x01),
    MEMORY(0x02),
    GLOBAL(0x03),
    TAG(0x04);

    private final int id;

    @SuppressWarnings("EnumOrdinal")
    ExternalType(int id) {
        this.id = id;
        assert ordinal() == id;
    }

    /**
     * @return the numerical identifier for this external kind
     */
    public int id() {
        return id;
    }

    private static final List<ExternalType> values = List.of(values());

    public static ExternalType byId(int id) {
        return values.get(id);
    }
}
