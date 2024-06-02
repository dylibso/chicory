package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * An imported global variable or constant.
 */
public final class GlobalImport extends Import {
    private final MutabilityType mutabilityType;
    private final ValueType type;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported global name (must not be {@code null})
     * @param mutabilityType the mutability type of the global (must not be {@code null})
     * @param type the type of the value stored in the global (must not be {@code null})
     */
    public GlobalImport(
            final String moduleName, final String name, final MutabilityType mutabilityType, final ValueType type) {
        super(moduleName, name);
        this.mutabilityType = Objects.requireNonNull(mutabilityType, "mutabilityType");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * {@return the mutability type of the global}
     */
    public MutabilityType mutabilityType() {
        return mutabilityType;
    }

    /**
     * {@return the type of the value stored in the global}
     */
    public ValueType type() {
        return type;
    }

    public ExternalType importType() {
        return ExternalType.GLOBAL;
    }

    public boolean equals(final Import other) {
        return other instanceof GlobalImport && equals((GlobalImport) other);
    }

    public boolean equals(final GlobalImport other) {
        return this == other || super.equals(other) && mutabilityType == other.mutabilityType && type == other.type;
    }

    public int hashCode() {
        return (super.hashCode() * 19 + mutabilityType.hashCode()) * 19 + type.hashCode();
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("global (type=")
                .append(type)
                .append(",mut=")
                .append(mutabilityType)
                .append(')');
        return super.toString(b);
    }
}
