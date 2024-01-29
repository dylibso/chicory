package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction that models a memory access.
 */
public final class MemoryAccessInsn extends Insn<Op.MemoryAccess> implements Cacheable {
    private final int memory;
    private final long offset;
    private final int alignment;

    MemoryAccessInsn(Op.MemoryAccess op, int memory, long offset, int alignment) {
        super(
                op,
                (memory * 31 + Long.hashCode(offset)) * 31
                        + Integer.numberOfTrailingZeros(alignment));
        this.memory = memory;
        this.offset = offset;
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment (must be a power of 2)");
        }
        this.alignment = alignment;
    }

    public int memory() {
        return memory;
    }

    public long offset() {
        return offset;
    }

    /**
     * {@return the access alignment, in bytes}
     * This value is always a power of two.
     */
    public int alignment() {
        return alignment;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryAccessInsn && equals((MemoryAccessInsn) obj);
    }

    public boolean equals(MemoryAccessInsn other) {
        return this == other
                || super.equals(other)
                        && memory == other.memory
                        && offset == other.offset
                        && alignment == other.alignment;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        if (memory == 0) {
            out.u31(Integer.numberOfTrailingZeros(alignment));
        } else {
            out.u31(Integer.numberOfTrailingZeros(alignment | 0b1000000));
            out.u31(memory);
        }
        out.u31(offset);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return memarg(memidx(super.toString(sb), memory), offset, alignment, op().alignment());
    }
}
