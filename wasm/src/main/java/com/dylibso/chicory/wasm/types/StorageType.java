package com.dylibso.chicory.wasm.types;

public class StorageType {
    private final ValType valType;
    private final PackedType packedType;

    // TODO: Builders
    public StorageType(ValType valType, PackedType packedType) {
        this.valType = valType;
        this.packedType = packedType;
    }

    public ValType getValType() {
        return valType;
    }

    public PackedType getPackedType() {
        return packedType;
    }
}
