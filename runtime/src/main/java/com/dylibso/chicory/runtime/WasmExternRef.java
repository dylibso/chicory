package com.dylibso.chicory.runtime;

/**
 * Wrapper for externref values converted to anyref via any.convert_extern.
 * Prevents GC ref ID collisions between extern values and native GC objects.
 */
public final class WasmExternRef implements WasmGcRef {

    private static final int ANY_HEAP_TYPE = -18; // ValType.TypeIdxCode.ANY.code()

    private final long value;

    public WasmExternRef(long value) {
        this.value = value;
    }

    @Override
    public int typeIdx() {
        return ANY_HEAP_TYPE;
    }

    public long value() {
        return value;
    }
}
