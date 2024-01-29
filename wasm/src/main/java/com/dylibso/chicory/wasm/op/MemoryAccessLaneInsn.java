package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction that models a memory access involving a vector lane.
 */
public final class MemoryAccessLaneInsn extends Insn<Op.MemoryAccessLane> implements Cacheable {
    private final int memory;
    private final long offset;
    private final int alignment;
    private final int lane;

    MemoryAccessLaneInsn(Op.MemoryAccessLane op, int memory, long offset, int alignment, int lane) {
        super(op, ((memory * 31 + Long.hashCode(offset)) * 31 + alignment) * 31 + lane);
        this.memory = memory;
        this.offset = offset;
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment (must be a power of 2)");
        }
        this.alignment = alignment;
        this.lane = lane;
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

    public int lane() {
        return lane;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MemoryAccessLaneInsn && equals((MemoryAccessLaneInsn) obj);
    }

    public boolean equals(MemoryAccessLaneInsn other) {
        return this == other
                || super.equals(other)
                        && memory == other.memory
                        && offset == other.offset
                        && alignment == other.alignment
                        && lane == other.lane;
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
        out.u8(lane);
        throw new UnsupportedOperationException();
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        memidx(sb, memory);
        memarg(sb, offset, alignment, op().alignment());
        return sb.append(' ').append(lane);
    }
}
