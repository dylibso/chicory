package com.dylibso.chicory.wasm.types;

/**
 * An imported function.
 */
public final class FunctionImport extends Import {
    private final int typeIndex;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported function name (must not be {@code null})
     * @param typeIndex the type index of the function (should correspond to a valid index in the type section)
     */
    public FunctionImport(String moduleName, String name, int typeIndex) {
        super(moduleName, name);
        this.typeIndex = typeIndex;
    }

    /**
     * Returns the index into the Type Section that defines the signature of this imported function.
     *
     * @return the type index.
     */
    public int typeIndex() {
        return typeIndex;
    }

    /**
     * Returns the external type, which is always {@link ExternalType#FUNCTION}.
     *
     * @return {@link ExternalType#FUNCTION}.
     */
    @Override
    public ExternalType importType() {
        return ExternalType.FUNCTION;
    }

    /**
     * Compares this function import to another import.
     *
     * @param other the object to compare against.
     * @return {@code true} if the other object is a {@code FunctionImport} and is equal to this one, {@code false} otherwise.
     */
    @Override
    public boolean equals(Import other) {
        return other instanceof FunctionImport && equals((FunctionImport) other);
    }

    /**
     * Compares this function import to another function import for equality.
     * Equality is based on module name, name, and type index.
     *
     * @param other the {@code FunctionImport} to compare against.
     * @return {@code true} if the imports are equal, {@code false} otherwise.
     */
    public boolean equals(FunctionImport other) {
        return this == other || super.equals(other) && typeIndex == other.typeIndex;
    }

    /**
     * Computes the hash code for this function import.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 19 + typeIndex;
    }

    /**
     * Appends a string representation of this function import to the given {@link StringBuilder}.
     *
     * @param b the {@link StringBuilder} to append to.
     * @return the modified {@link StringBuilder}.
     */
    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("func (type=").append(typeIndex).append(')');
        return super.toString(b);
    }
}
