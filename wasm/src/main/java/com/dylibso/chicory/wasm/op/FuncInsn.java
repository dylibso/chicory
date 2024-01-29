package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a function as its argument.
 */
public final class FuncInsn extends Insn<Op.Func> implements Cacheable {
    private final int func;

    FuncInsn(Op.Func op, int func) {
        super(op, func);
        this.func = func;
    }

    public int func() {
        return func;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FuncInsn && equals((FuncInsn) obj);
    }

    public boolean equals(FuncInsn other) {
        return this == other || super.equals(other) && func == other.func;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(func);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(func);
    }
}
