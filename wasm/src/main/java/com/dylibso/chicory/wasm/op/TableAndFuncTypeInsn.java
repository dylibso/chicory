package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on a table.
 */
public final class TableAndFuncTypeInsn extends Insn<Op.TableAndFuncType> implements Cacheable {
    private final int table;
    private final int type;

    TableAndFuncTypeInsn(Op.TableAndFuncType op, int table, int type) {
        super(op, table * 31 + type);
        this.table = table;
        this.type = type;
    }

    public int table() {
        return table;
    }

    public int type() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TableAndFuncTypeInsn && equals((TableAndFuncTypeInsn) obj);
    }

    public boolean equals(TableAndFuncTypeInsn other) {
        return this == other || super.equals(other) && table == other.table && type == other.type;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(type);
        out.u31(table);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(table).append(' ').append(type);
    }
}
