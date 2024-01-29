package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction that models an atomic memory access.
 */
public final class AtomicMemoryAccessInsn extends Insn<Op.AtomicMemoryAccess> implements Cacheable {
    private final int memory;
    private final long offset;

    AtomicMemoryAccessInsn(Op.AtomicMemoryAccess op, int memory, long offset) {
        super(op, memory * 31 + Long.hashCode(offset));
        this.memory = memory;
        this.offset = offset;
    }

    /**
     * {@return the memory to use for the operation}
     */
    public int memory() {
        return memory;
    }

    /**
     * {@return the offset to use for the operation}
     */
    public long offset() {
        return offset;
    }

    /**
     * {@return the access alignment, in bytes}
     * This value is always a power of two.
     */
    public int alignment() {
        return op().alignment();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AtomicMemoryAccessInsn && equals((AtomicMemoryAccessInsn) obj);
    }

    public boolean equals(AtomicMemoryAccessInsn other) {
        return this == other
                || super.equals(other) && memory == other.memory && offset == other.offset;
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        memidx(sb, memory);
        return sb.append(' ').append(offset);
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(memory);
        out.u32(offset);
    }
}
