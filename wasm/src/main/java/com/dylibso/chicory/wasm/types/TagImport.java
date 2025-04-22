package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * Represents an imported tag (exception type) in a WebAssembly module,
 * based on the Exception Handling proposal.
 */
public final class TagImport extends Import {
    private final TagType tagType;

    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the imported tag name (must not be {@code null})
     * @param attribute the tag attribute (must be 0x00)
     * @param tagTypeIdx the index into the Type section representing the function type of the tag
     */
    public TagImport(String moduleName, String name, byte attribute, int tagTypeIdx) {
        super(moduleName, name);
        this.tagType = new TagType(attribute, tagTypeIdx);
    }

    /**
     * Returns the type information associated with this imported tag.
     *
     * @return the {@link TagType}.
     */
    public TagType tagType() {
        return tagType;
    }

    /**
     * Returns the external type, which is always {@link ExternalType#TAG}.
     *
     * @return {@link ExternalType#TAG}.
     */
    @Override
    public ExternalType importType() {
        return ExternalType.TAG;
    }

    /**
     * Compares this tag import to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code TagImport} with the same module name, name, and tag type, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TagImport tagImport = (TagImport) o;
        return Objects.equals(tagType, tagImport.tagType);
    }

    /**
     * Computes the hash code for this tag import.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagType);
    }

    /**
     * Returns a string representation of this tag import.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "TagImport{" + "tagType=" + tagType + '}';
    }
}
