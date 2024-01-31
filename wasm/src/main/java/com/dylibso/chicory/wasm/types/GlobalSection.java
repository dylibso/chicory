package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class GlobalSection extends Section {
    private final ArrayList<Global> globals;

    private GlobalSection(ArrayList<Global> globals) {
        super(SectionId.GLOBAL);
        this.globals = globals;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(GlobalSection globalSection) {
        return new Builder(globalSection);
    }

    public Global[] globals() {
        return globals.toArray(Global[]::new);
    }

    public int globalCount() {
        return globals.size();
    }

    public Global getGlobal(int idx) {
        return globals.get(idx);
    }

    public static final class Builder {

        private final ArrayList<Global> globals;

        private Builder() {
            this.globals = new ArrayList<>();
        }

        private Builder(GlobalSection globalSection) {
            this.globals = new ArrayList<>();
            this.globals.addAll(globalSection.globals);
        }

        public Builder addGlobal(Global global) {
            Objects.requireNonNull(global, "global");
            globals.add(global);
            return this;
        }

        public GlobalSection build() {
            return new GlobalSection(this.globals);
        }
    }
}
