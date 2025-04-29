package com.dylibso.chicory.wasm.types;

public enum WasmEncoding {
    VARUINT,
    VARSINT32,
    VARSINT64,
    FLOAT32,
    FLOAT64,
    VEC_VARUINT,
    VEC_CATCH,
    BYTE,
    V128,
    BLOCK_TYPE,
    VALUE_TYPE,
    VEC_VALUE_TYPE
}
