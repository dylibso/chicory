package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GlobalSection extends Section {
    private final List<Global> globals;

    private GlobalSection(List<Global> globals) {
        super(SectionId.GLOBAL);
        this.globals = List.copyOf(globals);
    }

    public Global[] globals() {
        return globals.toArray(new Global[0]);
    }

    public int globalCount() {
        return globals.size();
    }

    public Global getGlobal(int idx) {
        return globals.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Global> globals = new ArrayList<>();

        private Builder() {}

        /**
         * Add a global variable definition to this section.
         *
         * @param global the global to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addGlobal(Global global) {
            Objects.requireNonNull(global, "global");
            globals.add(global);
            return this;
        }

        public GlobalSection build() {
            return new GlobalSection(globals);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof GlobalSection)) {
            return false;
        }
        GlobalSection that = (GlobalSection) o;
        return Objects.equals(globals, that.globals);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(globals);
    }
}
