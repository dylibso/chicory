package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * An imported table.
 */
public final class TagImport extends Import {
    private final TagType tagType;

    public TagImport(String moduleName, String name, byte attribute, int tagTypeIdx) {
        super(moduleName, name);
        this.tagType = new TagType(attribute, tagTypeIdx);
    }

    public TagType tagType() {
        return tagType;
    }

    @Override
    public ExternalType importType() {
        return ExternalType.TAG;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagType);
    }

    @Override
    public String toString() {
        return "TagImport{" + "tagType=" + tagType + '}';
    }
}
