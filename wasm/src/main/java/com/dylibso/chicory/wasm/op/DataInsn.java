package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which deals with a data segment.
 */
public final class DataInsn extends Insn<Op.Data> implements Cacheable {
    private final int segment;

    DataInsn(Op.Data op, int segment) {
        super(op, segment);
        this.segment = segment;
    }

    public int segment() {
        return segment;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DataInsn && equals((DataInsn) obj);
    }

    public boolean equals(DataInsn other) {
        return this == other || super.equals(other) && segment == other.segment;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(segment);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(segment);
    }
}
