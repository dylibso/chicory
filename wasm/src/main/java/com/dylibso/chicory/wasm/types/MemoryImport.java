package com.dylibso.chicory.wasm.types;

/**
 * An imported memory.
 */
public final class MemoryImport extends Import {
    private final MemoryLimits limits;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported memory name (must not be {@code null})
     * @param limits the memory size limits (must not be {@code null})
     */
    public MemoryImport(String moduleName, String name, MemoryLimits limits) {
        super(moduleName, name);
        this.limits = limits;
    }

    /**
     * Returns the size limits (initial and optional maximum) of this imported memory.
     *
     * @return the {@link MemoryLimits}.
     */
    public MemoryLimits limits() {
        return limits;
    }

    /**
     * Returns the external type, which is always {@link ExternalType#MEMORY}.
     *
     * @return {@link ExternalType#MEMORY}.
     */
    @Override
    public ExternalType importType() {
        return ExternalType.MEMORY;
    }

    /**
     * Compares this memory import to another import.
     *
     * @param other the object to compare against.
     * @return {@code true} if the other object is a {@code MemoryImport} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Import other) {
        return other instanceof MemoryImport && equals((MemoryImport) other);
    }

    /**
     * Compares this memory import to another memory import for equality.
     * Equality is based on module name, name, and limits.
     *
     * @param other the {@code MemoryImport} to compare against.
     * @return {@code true} if the imports are equal, {@code false} otherwise.
     */
    public boolean equals(MemoryImport other) {
        return this == other || super.equals(other) && limits.equals(other.limits);
    }

    /**
     * Computes the hash code for this memory import.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 19 + limits.hashCode();
    }

    /**
     * Appends a string representation of this memory import to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("memory (limits=");
        limits.toString(b);
        b.append(')');
        return super.toString(b);
    }
}
