package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction that operates on a lane index.
 */
public final class LaneInsn extends Insn<Op.Lane> implements Cacheable {
    private final int laneIdx;

    LaneInsn(Op.Lane op, int laneIdx) {
        super(op, laneIdx);
        this.laneIdx = laneIdx;
    }

    public int laneIdx() {
        return laneIdx;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LaneInsn && equals((LaneInsn) obj);
    }

    public boolean equals(LaneInsn other) {
        return this == other || super.equals(other) && laneIdx == other.laneIdx;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u8(laneIdx);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(laneIdx);
    }
}
