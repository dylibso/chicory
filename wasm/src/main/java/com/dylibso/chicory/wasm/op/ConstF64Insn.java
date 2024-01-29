package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public final class ConstF64Insn extends Insn<Op.ConstF64> implements Cacheable {
    private final double val;

    ConstF64Insn(Op.ConstF64 op, double val) {
        super(op, Long.hashCode(Double.doubleToRawLongBits(val)));
        this.val = val;
    }

    public double val() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstF64Insn && equals((ConstF64Insn) obj);
    }

    public boolean equals(ConstF64Insn other) {
        return this == other || super.equals(other) && bitEquals(val, other.val);
    }

    private static boolean bitEquals(final double d1, final double d2) {
        return Double.doubleToRawLongBits(d1) == Double.doubleToRawLongBits(d2);
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.f64(val);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb).append(' ');
        if (Double.isNaN(val)) {
            sb.append("nan");
            // todo: add nan payload bits
        } else if (Double.isInfinite(val)) {
            if (val < 0) {
                sb.append('-');
            }
            sb.append("inf");
        } else {
            sb.append(Double.toHexString(val));
        }
        return sb;
    }
}
