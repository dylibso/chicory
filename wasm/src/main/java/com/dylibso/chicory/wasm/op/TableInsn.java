package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on a table.
 */
public final class TableInsn extends Insn<Op.Table> implements Cacheable {
    private final int table;

    TableInsn(Op.Table op, int table) {
        super(op, table);
        this.table = table;
    }

    public int table() {
        return table;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TableInsn && equals((TableInsn) obj);
    }

    public boolean equals(TableInsn other) {
        return this == other || super.equals(other) && table == other.table;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(table);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(table);
    }
}
