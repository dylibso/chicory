package com.dylibso.chicory.wasm.types;

public class FieldType {
    private final StorageType storageType;
    private final MutabilityType mut;

    public FieldType(StorageType storageType, MutabilityType mut) {
        this.storageType = storageType;
        this.mut = mut;
    }

    public StorageType storageType() {
        return storageType;
    }

    public MutabilityType mut() {
        return mut;
    }
}
