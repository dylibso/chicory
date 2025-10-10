package com.dylibso.chicory.wasm.types;

public class StorageType {
    private final ValType valType;
    private final PackedType packedType;

    public StorageType(ValType valType, PackedType packedType) {
        this.valType = valType;
        this.packedType = packedType;
    }

    public ValType valType() {
        return valType;
    }

    public PackedType packedType() {
        return packedType;
    }
}
