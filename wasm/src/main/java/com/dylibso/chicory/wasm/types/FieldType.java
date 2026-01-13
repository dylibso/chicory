package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public final class FieldType {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldType fieldType = (FieldType) o;
        return Objects.equals(storageType, fieldType.storageType) && mut == fieldType.mut;
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageType, mut);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StorageType storageType;
        private MutabilityType mut;

        private Builder() {}

        public Builder withStorageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

        public Builder withMutability(MutabilityType mut) {
            this.mut = mut;
            return this;
        }

        public FieldType build() {
            return new FieldType(storageType, mut);
        }
    }
}
