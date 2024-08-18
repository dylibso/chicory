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
     * @return the type index corresponding to the imported function's type
     */
    public int typeIndex() {
        return typeIndex;
    }

    @Override
    public ExternalType importType() {
        return ExternalType.FUNCTION;
    }

    @Override
    public boolean equals(Import other) {
        return other instanceof FunctionImport && equals((FunctionImport) other);
    }

    public boolean equals(FunctionImport other) {
        return this == other || super.equals(other) && typeIndex == other.typeIndex;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + typeIndex;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("func (type=").append(typeIndex).append(')');
        return super.toString(b);
    }
}
