package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import java.util.Objects;

/**
 * An instruction instance.
 */
public abstract class Insn<I extends Op> {
    /**
     * The operation.
     */
    private final I op;

    /**
     * The primary opcode value, cached for access speed.
     */
    private final int opcode;

    /**
     * The secondary opcode value, cached for access speed.
     */
    private final int secondaryOpcode;

    /**
     * The base hash code.
     */
    private final int hashCode;

    /**
     * The cached {@code toString} value.
     */
    private String toString;

    Insn(I op, int hashCode) {
        this.op = Objects.requireNonNull(op, "op");
        opcode = op.opcode();
        secondaryOpcode = op.secondaryOpcode();
        this.hashCode = Objects.hash(getClass(), op) * 31 + hashCode;
    }

    /**
     * {@return the operation of this instruction}
     */
    public final I op() {
        return op;
    }

    /**
     * {@return the primary opcode, which may be a prefix}
     */
    public final int opcode() {
        return opcode;
    }

    /**
     * {@return the secondary opcode, or -1 if there is no prefix for this instruction}
     */
    public final int secondaryOpcode() {
        return secondaryOpcode;
    }

    public abstract boolean equals(final Object obj);

    // called only by subclasses.
    final boolean equals(Insn<?> other) {
        return other != null && op == other.op && hashCode == other.hashCode;
    }

    /**
     * {@return the instruction hash code}
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * Append a readable string representation of this instruction to the given builder.
     *
     * @param sb the string builder (must not be {@code null})
     * @return the string builder that was passed in
     */
    public StringBuilder toString(StringBuilder sb) {
        return sb.append(op);
    }

    /**
     * {@return a readable string representation of this instruction}
     */
    public final String toString() {
        String toString = this.toString;
        if (toString == null) {
            toString = this.toString = toString(new StringBuilder()).toString();
        }
        return toString;
    }

    /**
     * Write this instruction to the given stream.
     *
     * @param out the stream to write to (must not be {@code null})
     * @throws WasmIOException if an I/O error occurs
     */
    public void writeTo(WasmOutputStream out) throws WasmIOException {
        out.op(op);
    }

    static StringBuilder memidx(StringBuilder sb, int memory) {
        if (memory != 0) {
            sb.append(' ').append(memory);
        }
        return sb;
    }

    static StringBuilder memarg(StringBuilder sb, long offset, int align, int opAlignment) {
        if (offset != 0) {
            sb.append(' ').append("offset=").append(offset);
        }
        if (align != opAlignment) {
            // todo: is alignment written as log2(alignment)?
            // todo: only omit for the natural size of the instruction
            sb.append(' ').append("align=").append(align);
        }
        return sb;
    }
}
