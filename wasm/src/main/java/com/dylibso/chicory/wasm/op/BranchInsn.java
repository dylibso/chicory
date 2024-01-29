package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which branches to some enclosing label.
 */
public final class BranchInsn extends Insn<Op.Branch> implements Cacheable {
    private final int target;

    BranchInsn(Op.Branch op, int target) {
        super(op, target);
        this.target = target;
    }

    public int target() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BranchInsn && equals((BranchInsn) obj);
    }

    public boolean equals(BranchInsn other) {
        return this == other || super.equals(other) && target == other.target;
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(target);
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(target);
    }
}
