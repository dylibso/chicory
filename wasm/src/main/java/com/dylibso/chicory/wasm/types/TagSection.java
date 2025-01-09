package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TagSection extends Section {
    private final List<TagType> tags;

    private TagSection(List<TagType> tags) {
        super(SectionId.TAG);
        this.tags = List.copyOf(tags);
    }

    public TagType[] types() {
        return tags.toArray(TagType[]::new);
    }

    public int tagCount() {
        return tags.size();
    }

    public TagType getTag(int idx) {
        return tags.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<TagType> tags = new ArrayList<>();

        private Builder() {}

        public Builder addTagType(TagType tagType) {
            Objects.requireNonNull(tagType, "tagType");
            tags.add(tagType);
            return this;
        }

        public TagSection build() {
            return new TagSection(tags);
        }
    }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(tags);
    }
}
