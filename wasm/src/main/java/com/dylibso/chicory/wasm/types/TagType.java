package com.dylibso.chicory.wasm.types;

/**
 * Represents the type of a tag (exception), defined in the Tag Section
 * of a WebAssembly module, as part of the Exception Handling proposal.
 */
public class TagType {
    private final byte attribute;
    private final int typeIdx;

    /**
     * Constructs a new TagType.
     *
     * @param attribute the tag attribute (currently must be 0).
     * @param typeIdx the index into the Type section, defining the signature of the exception handler.
     */
    public TagType(byte attribute, int typeIdx) {
        this.attribute = attribute;
        this.typeIdx = typeIdx;
    }

    /**
     * Returns the attribute of the tag.
     * According to the current specification, this must always be 0.
     *
     * @return the tag attribute (0).
     */
    public byte attribute() {
        return attribute;
    }

    /**
     * Returns the index into the Type section.
     * This index points to a {@link FunctionType} that defines the parameter types
     * expected by handlers catching this type of exception.
     *
     * @return the type index.
     */
    public int typeIdx() {
        return typeIdx;
    }
}
