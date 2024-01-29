package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public final class ConstF32Insn extends Insn<Op.ConstF32> implements Cacheable {
    private final float val;

    ConstF32Insn(Op.ConstF32 op, float val) {
        super(op, Float.floatToRawIntBits(val));
        this.val = val;
    }

    public float val() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstF32Insn && equals((ConstF32Insn) obj);
    }

    public boolean equals(ConstF32Insn other) {
        return this == other || super.equals(other) && bitEquals(val, other.val);
    }

    private static boolean bitEquals(float v1, float v2) {
        return Float.floatToRawIntBits(v1) == Float.floatToRawIntBits(v2);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb).append(' ');
        if (Float.isNaN(val)) {
            sb.append("nan");
            // todo: add nan payload bits
        } else if (Float.isInfinite(val)) {
            if (val < 0) {
                sb.append('-');
            }
            sb.append("inf");
        } else {
            sb.append(Float.toHexString(val));
        }
        return sb;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.f32(val);
    }
}
