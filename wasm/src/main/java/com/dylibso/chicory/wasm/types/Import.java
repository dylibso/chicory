package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * Some imported entity.
 * <p>
 * See <a href=https://webassembly.github.io/spec/core/syntax/modules.html#syntax-import">Imports</a> for
 * reference.
 */
public abstract class Import {
    private final String moduleName;
    private final String name;

    Import(String moduleName, String name) {
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * {@return the module name to import from}
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * {@return the import name}
     */
    public String name() {
        return name;
    }

    /**
     * {@return the kind of imported definition}
     */
    public abstract ExternalType importType();

    public boolean equals(Object obj) {
        return obj instanceof Import && equals((Import) obj);
    }

    public boolean equals(Import other) {
        return other != null && moduleName.equals(other.moduleName) && name.equals(other.name);
    }

    public int hashCode() {
        return Objects.hash(moduleName, name);
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append('<').append(moduleName).append('.').append(name).append('>');
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
