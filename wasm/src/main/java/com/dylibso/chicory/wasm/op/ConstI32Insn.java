package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public final class ConstI32Insn extends Insn<Op.ConstI32> implements Cacheable {
    private final int val;

    ConstI32Insn(Op.ConstI32 op, int val) {
        super(op, val);
        this.val = val;
    }

    public int val() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstI32Insn && equals((ConstI32Insn) obj);
    }

    public boolean equals(ConstI32Insn other) {
        return this == other || super.equals(other) && val == other.val;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.s32(val);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(val);
    }
}
