package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on a tag.
 */
public final class TagInsn extends Insn<Op.Tag> implements Cacheable {
    private final int tag;

    TagInsn(Op.Tag op, int tag) {
        super(op, tag);
        this.tag = tag;
    }

    public int tag() {
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TagInsn && equals((TagInsn) obj);
    }

    public boolean equals(TagInsn other) {
        return this == other || super.equals(other) && tag == other.tag;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(tag);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(tag);
    }
}
