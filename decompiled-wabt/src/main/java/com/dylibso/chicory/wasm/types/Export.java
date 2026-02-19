package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * An exported definition.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-export">Exports</a> for
 * reference.
 */
public class Export {
    private final String name;
    private final int index;
    private final ExternalType exportType;

    /**
     * Construct a new instance.
     * The index is interpreted according to {@code type}.
     *
     * @param name the export name (must not be {@code null})
     * @param index the index of the definition to export
     * @param exportType the export type (must not be {@code null})
     */
    public Export(String name, int index, ExternalType exportType) {
        this.name = Objects.requireNonNull(name, "name");
        this.index = index;
        this.exportType = Objects.requireNonNull(exportType, "type");
    }

    /**
     * @return the export name
     */
    public String name() {
        return name;
    }

    /**
     * @return the export index
     */
    public int index() {
        return index;
    }

    /**
     * @return the type of exported definition
     */
    public ExternalType exportType() {
        return exportType;
    }

    @Override
    public int hashCode() {
        return (name.hashCode() * 31 + index) * 31 + exportType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Export && equals((Export) obj);
    }

    public boolean equals(Export other) {
        return this == other
                || other != null
                        && index == other.index
                        && exportType == other.exportType
                        && name.equals(other.name);
    }
}
