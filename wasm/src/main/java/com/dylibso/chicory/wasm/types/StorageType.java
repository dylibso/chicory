package com.dylibso.chicory.wasm.types;

public final class StorageType {
    private final ValType valType;
    private final PackedType packedType;

    private StorageType(ValType valType, PackedType packedType) {
        this.valType = valType;
        this.packedType = packedType;
    }

    public ValType valType() {
        return valType;
    }

    public PackedType packedType() {
        return packedType;
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
