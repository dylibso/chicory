package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An exception-throwing instruction.
 */
public final class ExceptionInsn extends Insn<Op.Exception> implements Cacheable {
    private final int target;

    ExceptionInsn(Op.Exception op, int target) {
        super(op, target);
        this.target = target;
    }

    public int target() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ExceptionInsn && equals((ExceptionInsn) obj);
    }

    public boolean equals(ExceptionInsn other) {
        return this == other || super.equals(other) && target == other.target;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(target);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(target);
    }
}
