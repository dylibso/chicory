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
            String moduleName, String name, MutabilityType mutabilityType, ValueType type) {
        super(moduleName, name);
        this.mutabilityType = Objects.requireNonNull(mutabilityType, "mutabilityType");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns the mutability type (Const or Var) of this imported global.
     *
     * @return the {@link MutabilityType}.
     */
    public MutabilityType mutabilityType() {
        return mutabilityType;
    }

    /**
     * Returns the value type of this imported global.
     *
     * @return the {@link ValueType}.
     */
    public ValueType type() {
        return type;
    }

    /**
     * Returns the external type, which is always {@link ExternalType#GLOBAL}.
     *
     * @return {@link ExternalType#GLOBAL}.
     */
    @Override
    public ExternalType importType() {
        return ExternalType.GLOBAL;
    }

    /**
     * Compares this global import to another import.
     *
     * @param other the object to compare against.
     * @return {@code true} if the other object is a {@code GlobalImport} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Import other) {
        return other instanceof GlobalImport && equals((GlobalImport) other);
    }

    /**
     * Compares this global import to another global import for equality.
     * Equality is based on module name, name, mutability type, and value type.
     *
     * @param other the {@code GlobalImport} to compare against.
     * @return {@code true} if the imports are equal, {@code false} otherwise.
     */
    public boolean equals(GlobalImport other) {
        return this == other
                || super.equals(other)
                        && mutabilityType == other.mutabilityType
                        && type == other.type;
    }

    /**
     * Computes the hash code for this global import.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return (super.hashCode() * 19 + mutabilityType.hashCode()) * 19 + type.hashCode();
    }

    /**
     * Appends a string representation of this global import to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("global (type=").append(type).append(",mut=").append(mutabilityType).append(')');
        return super.toString(b);
    }
}
