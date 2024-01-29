package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public final class ConstI64Insn extends Insn<Op.ConstI64> implements Cacheable {
    private final long val;

    ConstI64Insn(Op.ConstI64 op, long val) {
        super(op, Long.hashCode(val));
        this.val = val;
    }

    public long val() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstI64Insn && equals((ConstI64Insn) obj);
    }

    public boolean equals(ConstI64Insn other) {
        return this == other || super.equals(other) && val == other.val;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.s64(val);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(val);
    }
}
