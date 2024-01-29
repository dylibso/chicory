package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on two tables (destination and source).
 */
public final class TableToTableInsn extends Insn<Op.TableToTable> implements Cacheable {
    private final int destination;
    private final int source;

    TableToTableInsn(Op.TableToTable op, int destination, int source) {
        super(op, destination * 31 + source);
        this.destination = destination;
        this.source = source;
    }

    public int destination() {
        return destination;
    }

    public int source() {
        return source;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TableToTableInsn && equals((TableToTableInsn) obj);
    }

    public boolean equals(TableToTableInsn other) {
        return this == other
                || super.equals(other)
                        && destination == other.destination
                        && source == other.source;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(destination);
        out.u31(source);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(destination).append(' ').append(source);
    }
}
