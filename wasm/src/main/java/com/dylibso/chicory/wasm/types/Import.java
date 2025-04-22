package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Some imported entity.
 * <p>
 * See <a href="https://webassembly.github.io/spec/core/syntax/modules.html#syntax-import">Imports</a> for
 * reference.
 */
public abstract class Import {
    private final String module;
    private final String name;

    Import(String module, String name) {
        this.module = requireNonNull(module, "moduleName");
        this.name = requireNonNull(name, "name");
    }

    /**
     * Returns the name of the module from which this entity is imported.
     *
     * @return the module name string.
     */
    public String module() {
        return module;
    }

    /**
     * Returns the name of the entity being imported.
     *
     * @return the import name string.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of the imported entity (e.g., Function, Table, Memory, Global, Tag).
     *
     * @return the {@link ExternalType} of the import.
     */
    public abstract ExternalType importType();

    /**
     * Compares this import to another object for equality.
     *
     * @param obj the object to compare against.
     * @return {@code true} if the object is an {@code Import} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Import && equals((Import) obj);
    }

    /**
     * Compares this import to another import for equality.
     * Base equality is based on module name and name.
     * Subclasses should override this to include type-specific fields.
     *
     * @param other the {@code Import} to compare against.
     * @return {@code true} if the imports have the same module and name, {@code false} otherwise.
     */
    public boolean equals(Import other) {
        return other != null && module.equals(other.module) && name.equals(other.name);
    }

    /**
     * Computes the hash code for this import.
     * The hash code is based on the module name and name.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(module, name);
    }

    /**
     * Appends a partial string representation of this import (module and name) to the given {@link StringBuilder}.
     * Subclasses should override this to add type-specific information.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    public StringBuilder toString(StringBuilder b) {
        return b.append('<').append(module).append('.').append(name).append('>');
    }

    /**
     * Returns a string representation of this import.
     *
     * @return a string representation of the import.
     */
    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
