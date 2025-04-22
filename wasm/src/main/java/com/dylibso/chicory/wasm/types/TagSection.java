package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Tag Section in a WebAssembly module, part of the Exception Handling proposal.
 * This section declares all tags (exception types) defined within the module.
 */
public final class TagSection extends Section {
    private final List<TagType> tags;

    private TagSection(List<TagType> tags) {
        super(SectionId.TAG);
        this.tags = List.copyOf(tags);
    }

    /**
     * Returns the tag types defined in this section.
     *
     * @return an array of {@link TagType} instances.
     */
    public TagType[] types() {
        return tags.toArray(new TagType[0]);
    }

    /**
     * Returns the number of tags defined in this section.
     *
     * @return the count of tags.
     */
    public int tagCount() {
        return tags.size();
    }

    /**
     * Returns the tag type at the specified index.
     *
     * @param idx the index of the tag type to retrieve.
     * @return the {@link TagType} at the given index.
     */
    public TagType getTag(int idx) {
        return tags.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link TagSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link TagSection} instances.
     */
    public static final class Builder {
        private final List<TagType> tags = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a tag type definition to this section.
         *
         * @param tagType the {@link TagType} to add (must not be {@code null}).
         * @return this builder instance.
         */
        public Builder addTagType(TagType tagType) {
            Objects.requireNonNull(tagType, "tagType");
            tags.add(tagType);
            return this;
        }

        /**
         * Constructs the {@link TagSection} instance from the added tag types.
         *
         * @return the built {@link TagSection}.
         */
        public TagSection build() {
            return new TagSection(tags);
        }
    }

    /**
     * Compares this tag section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code TagSection} with the same tag types, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TagSection)) {
            return false;
        }
        TagSection that = (TagSection) o;
        return Objects.equals(tags, that.tags);
    }

    /**
     * Computes the hash code for this tag section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(tags);
    }
}
