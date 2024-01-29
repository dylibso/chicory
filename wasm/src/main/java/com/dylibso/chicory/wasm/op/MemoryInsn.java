package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on some memory instance.
 */
public final class MemoryInsn extends Insn<Op.Memory> implements Cacheable {
    private final int memory;

    MemoryInsn(Op.Memory op, int memory) {
        super(op, memory);
        this.memory = memory;
    }

    public int memory() {
        return memory;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryInsn && equals((MemoryInsn) obj);
    }

    public boolean equals(MemoryInsn other) {
        return this == other || super.equals(other) && memory == other.memory;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(memory);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        return memidx(sb, memory);
    }
}
