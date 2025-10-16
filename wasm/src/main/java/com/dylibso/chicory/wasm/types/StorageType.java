package com.dylibso.chicory.wasm.types;

import java.util.function.Function;

public class StorageType {
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
        private ValType.Builder valTypeBuilder;
        private PackedType packedType;

        private Builder() {}

        public Builder withValTypeBuilder(ValType.Builder valTypeBuilder) {
            this.valTypeBuilder = valTypeBuilder;
            return this;
        }

        public Builder withPackedType(PackedType packedType) {
            this.packedType = packedType;
            return this;
        }

        public StorageType build(Function<Integer, RecType> context) {
            var valType = (valTypeBuilder == null) ? null : valTypeBuilder.build(context);
            return new StorageType(valType, packedType);
        }
    }
}
