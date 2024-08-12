package com.dylibso.chicory.wasm.types;

import java.util.Objects;

/**
 * A custom section which is unknown to the parser.
 */
public final class UnknownCustomSection extends CustomSection {
    private final String name;
    private final byte[] bytes;

    /**
     * Construct a new instance.
     *
     * @param name the name of the section (must not be {@code null})
     * @param bytes the section contents (must not be {@code null})
     */
    private UnknownCustomSection(final String name, final byte[] bytes) {
        super();
        this.name = Objects.requireNonNull(name, "name");
        this.bytes = bytes.clone();
    }

    public String name() {
        return name;
    }

    public byte[] bytes() {
        return bytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private byte[] bytes;

        private Builder() {}

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withBytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public UnknownCustomSection build() {
            return new UnknownCustomSection(name, bytes);
        }
    }
}
