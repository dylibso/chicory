package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on a global variable.
 */
public final class GlobalInsn extends Insn<Op.Global> implements Cacheable {
    private final int global;

    GlobalInsn(Op.Global op, int global) {
        super(op, global);
        this.global = global;
    }

    public int global() {
        return global;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GlobalInsn && equals((GlobalInsn) obj);
    }

    public boolean equals(GlobalInsn other) {
        return this == other || super.equals(other) && global == other.global;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(global);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(global);
    }
}
