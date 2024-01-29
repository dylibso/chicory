package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public final class ConstV128Insn extends Insn<Op.ConstV128> implements Cacheable {
    private final long low;
    private final long high;

    ConstV128Insn(Op.ConstV128 op, long low, long high) {
        super(op, Long.hashCode(low) ^ Long.hashCode(high));
        this.low = low;
        this.high = high;
    }

    public long low() {
        return low;
    }

    public long high() {
        return high;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstV128Insn && equals((ConstV128Insn) obj);
    }

    public boolean equals(ConstV128Insn other) {
        return this == other || super.equals(other) && low == other.low && high == other.high;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.raw64le(low);
        out.raw64le(high);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb).append(" i64x2 ");
        sb.append("0x").append(Long.toHexString(high));
        sb.append(' ');
        sb.append("0x").append(Long.toHexString(low));
        return sb;
    }
}
