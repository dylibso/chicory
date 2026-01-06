package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public final class StorageType {
    private final ValType valType;
    private final PackedType packedType;

    private static void requireExactlyOneNonNull(Object a, Object b) {
        if ((a == null ? 0 : 1) + (b == null ? 0 : 1) != 1) {
            throw new IllegalArgumentException("Exactly one field must be filled");
        }
    }

    private StorageType(ValType valType, PackedType packedType) {
        requireExactlyOneNonNull(valType, packedType);

        this.valType = valType;
        this.packedType = packedType;
    }

    public ValType valType() {
        return valType;
    }

    public PackedType packedType() {
        return packedType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageType that = (StorageType) o;
        return Objects.equals(valType, that.valType) && packedType == that.packedType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valType, packedType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ValType valType;
        private PackedType packedType;

        private Builder() {}

        public Builder withValType(ValType valType) {
            this.valType = valType;
            return this;
        }

        public Builder withPackedType(PackedType packedType) {
            this.packedType = packedType;
            return this;
        }

        public StorageType build() {
            return new StorageType(valType, packedType);
        }
    }
}
