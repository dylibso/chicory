package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Global Section in a WebAssembly module.
 * This section declares all global variables defined within the module, excluding imported globals.
 */
public final class GlobalSection extends Section {
    private final List<Global> globals;

    private GlobalSection(List<Global> globals) {
        super(SectionId.GLOBAL);
        this.globals = List.copyOf(globals);
    }

    /**
     * Returns the global variable definitions in this section.
     *
     * @return an array of {@link Global} instances.
     */
    public Global[] globals() {
        return globals.toArray(new Global[0]);
    }

    /**
     * Returns the number of global variables defined in this section.
     *
     * @return the count of globals.
     */
    public int globalCount() {
        return globals.size();
    }

    /**
     * Returns the global variable definition at the specified index.
     *
     * @param idx the index of the global to retrieve.
     * @return the {@link Global} definition at the given index.
     */
    public Global getGlobal(int idx) {
        return globals.get(idx);
    }

    /**
     * Creates a new builder for constructing a {@link GlobalSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link GlobalSection} instances.
     */
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

        /**
         * Constructs the {@link GlobalSection} instance from the added globals.
         *
         * @return the built {@link GlobalSection}.
         */
        public GlobalSection build() {
            return new GlobalSection(globals);
        }
    }

    /**
     * Compares this global section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is a {@code GlobalSection} with the same globals, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this global section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(globals);
    }
}
