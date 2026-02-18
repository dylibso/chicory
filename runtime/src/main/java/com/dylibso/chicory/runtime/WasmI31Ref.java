package com.dylibso.chicory.runtime;

/**
 * Boxed representation of an i31ref value for storage in int-typed containers (tables, globals).
 * On the stack, i31 values use an efficient tagged-long encoding (see {@link
 * com.dylibso.chicory.wasm.types.Value#encodeI31}). This class is only used when i31 values need
 * to pass through int-typed storage where the tag would be lost.
 */
public final class WasmI31Ref implements WasmGcRef {

    private static final int I31_HEAP_TYPE = -20; // ValType.TypeIdxCode.I31.code()

    private final int value;

    public WasmI31Ref(int value) {
        this.value = value & 0x7FFFFFFF; // 31-bit value
    }

    @Override
    public int typeIdx() {
        return I31_HEAP_TYPE;
    }

    public int value() {
        return value;
    }
}
