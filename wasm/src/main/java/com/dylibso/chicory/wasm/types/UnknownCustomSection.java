package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

/**
 * A custom section which is unknown to the parser.
 */
public final class UnknownCustomSection extends CustomSection {
    private final String name;
    private final byte[] bytes;

    private UnknownCustomSection(String name, byte[] bytes) {
        this.name = requireNonNull(name, "name");
        this.bytes = bytes.clone();
    }

    /**
     * Returns the name of this custom section.
     *
     * @return the section name string.
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the raw byte content of this custom section.
     * A clone of the internal byte array is returned to prevent modification.
     *
     * @return a copy of the raw byte data.
     */
    public byte[] bytes() {
        return bytes.clone();
    }

    /**
     * Creates a new builder for constructing an {@link UnknownCustomSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link UnknownCustomSection} instances.
     */
    public static final class Builder {
        private String name;
        private byte[] bytes;

        private Builder() {}

        /**
         * Sets the name for the custom section.
         *
         * @param name the section name (must not be {@code null}).
         * @return this builder instance.
         */
        public Builder withName(String name) {
            this.name = requireNonNull(name);
            return this;
        }

        /**
         * Sets the raw byte content for the custom section.
         * The provided byte array is stored directly; consider cloning if the original array might be modified.
         *
         * @param bytes the raw byte data (must not be {@code null}).
         * @return this builder instance.
         */
        public Builder withBytes(byte[] bytes) {
            this.bytes = requireNonNull(bytes);
            return this;
        }

        /**
         * Constructs the {@link UnknownCustomSection} instance.
         *
         * @return the built {@link UnknownCustomSection}.
         */
        public UnknownCustomSection build() {
            return new UnknownCustomSection(name, bytes);
        }
    }
}
