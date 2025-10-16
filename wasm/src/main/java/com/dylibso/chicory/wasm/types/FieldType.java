package com.dylibso.chicory.wasm.types;

import java.util.function.Function;

public class FieldType {
    private final StorageType storageType;
    private final MutabilityType mut;

    private FieldType(StorageType storageType, MutabilityType mut) {
        this.storageType = storageType;
        this.mut = mut;
    }

    public StorageType storageType() {
        return storageType;
    }

    public MutabilityType mut() {
        return mut;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StorageType.Builder storageTypeBuilder;
        private MutabilityType mut;

        private Builder() {}

        public Builder withStorageTypeBuilder(StorageType.Builder storageTypeBuilder) {
            this.storageTypeBuilder = storageTypeBuilder;
            return this;
        }

        public Builder withMutability(MutabilityType mut) {
            this.mut = mut;
            return this;
        }

        public FieldType build(Function<Integer, RecType> context) {
            return new FieldType(storageTypeBuilder.build(context), mut);
        }
    }
}
