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

    @Override
    public String name() {
        return name;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private byte[] bytes;

        private Builder() {}

        public Builder withName(String name) {
            this.name = requireNonNull(name);
            return this;
        }

        public Builder withBytes(byte[] bytes) {
            this.bytes = requireNonNull(bytes);
            return this;
        }

        public UnknownCustomSection build() {
            return new UnknownCustomSection(name, bytes);
        }
    }
}
