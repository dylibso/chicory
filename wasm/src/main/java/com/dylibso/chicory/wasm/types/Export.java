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
     * Returns the external name under which this definition is exported.
     *
     * @return the export name string.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the index of the exported definition within its corresponding index space
     * (e.g., function index space, table index space).
     *
     * @return the index of the exported item.
     */
    public int index() {
        return index;
    }

    /**
     * Returns the type of the exported definition (e.g., Function, Table, Memory, Global, Tag).
     *
     * @return the {@link ExternalType} of the export.
     */
    public ExternalType exportType() {
        return exportType;
    }

    /**
     * Computes the hash code for this export.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return (name.hashCode() * 31 + index) * 31 + exportType.hashCode();
    }

    /**
     * Compares this export to another object for equality.
     *
     * @param obj the object to compare against.
     * @return {@code true} if the object is an {@code Export} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Export && equals((Export) obj);
    }

    /**
     * Compares this export to another export for equality.
     * Equality is based on name, index, and export type.
     *
     * @param other the {@code Export} to compare against.
     * @return {@code true} if the exports are equal, {@code false} otherwise.
     */
    public boolean equals(Export other) {
        return this == other
                || other != null
                        && index == other.index
                        && exportType == other.exportType
                        && name.equals(other.name);
    }
}
