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
    private final String moduleName;
    private final String name;

    Import(String moduleName, String name) {
        this.moduleName = requireNonNull(moduleName, "moduleName");
        this.name = requireNonNull(name, "name");
    }

    /**
     * @return the module name to import from
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * @return the import name
     */
    public String name() {
        return name;
    }

    /**
     * @return the kind of imported definition
     */
    public abstract ExternalType importType();

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Import && equals((Import) obj);
    }

    public boolean equals(Import other) {
        return other != null && moduleName.equals(other.moduleName) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, name);
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append('<').append(moduleName).append('.').append(name).append('>');
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
