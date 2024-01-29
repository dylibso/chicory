package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on a local variable.
 */
public final class LocalInsn extends Insn<Op.Local> implements Cacheable {
    private final int local;

    LocalInsn(Op.Local op, int local) {
        super(op, local);
        this.local = local;
    }

    public int local() {
        return local;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalInsn && equals((LocalInsn) obj);
    }

    public boolean equals(LocalInsn other) {
        return this == other || super.equals(other) && local == other.local;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(local);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(local);
    }
}
