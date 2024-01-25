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
    public MemoryImport(final String moduleName, final String name, final MemoryLimits limits) {
        super(moduleName, name);
        this.limits = limits;
    }

    /**
     * {@return the memory size limits, in pages}
     */
    public MemoryLimits limits() {
        return limits;
    }

    public ImportDescType descType() {
        return ImportDescType.MemIdx;
    }

    public boolean equals(final Import other) {
        return other instanceof MemoryImport && equals((MemoryImport) other);
    }

    public boolean equals(final MemoryImport other) {
        return this == other || super.equals(other) && limits.equals(other.limits);
    }

    public int hashCode() {
        return super.hashCode() * 19 + limits.hashCode();
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("memory (limits=");
        limits.toString(b);
        b.append(')');
        return super.toString(b);
    }
}
