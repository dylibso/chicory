package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on some memory instance and a data segment.
 */
public final class MemoryAndDataInsn extends Insn<Op.MemoryAndData> implements Cacheable {
    private final int memory;
    private final int segment;

    MemoryAndDataInsn(Op.MemoryAndData op, int memory, int segment) {
        super(op, memory * 31 + segment);
        this.memory = memory;
        this.segment = segment;
    }

    public int memory() {
        return memory;
    }

    public int segment() {
        return segment;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryAndDataInsn && equals((MemoryAndDataInsn) obj);
    }

    public boolean equals(MemoryAndDataInsn other) {
        return this == other
                || super.equals(other) && memory == other.memory && segment == other.segment;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(segment);
        out.u31(memory);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        memidx(sb, memory);
        return sb.append(' ').append(segment);
    }
}
