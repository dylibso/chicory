package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeSection extends Section {
    private final List<RecType> types;

    private TypeSection(List<RecType> types) {
        super(SectionId.TYPE);
        this.types = List.copyOf(types);
    }

    public RecType[] types() {
        return types.toArray(new RecType[0]);
    }

    public int typeCount() {
        return types.size();
    }

    public RecType getType(int idx) {
        return types.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RecType> types = new ArrayList<>();

        private Builder() {}

        public List<RecType> getTypes() {
            return types;
        }

        public Builder addRecTypes(RecType[] recTypes) {
            Objects.requireNonNull(recTypes, "functionType");
            types.addAll(List.of(recTypes));
            return this;
        }

        public TypeSection build() {
            return new TypeSection(types);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof TypeSection)) {
            return false;
        }
        TypeSection that = (TypeSection) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(types);
    }
}
