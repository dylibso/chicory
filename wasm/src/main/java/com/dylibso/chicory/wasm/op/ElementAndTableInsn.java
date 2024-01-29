package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which operates on an element and a table.
 */
public final class ElementAndTableInsn extends Insn<Op.ElementAndTable> implements Cacheable {
    private final int element;
    private final int table;

    ElementAndTableInsn(Op.ElementAndTable op, int element, int table) {
        super(op, element * 31 + table);
        this.element = element;
        this.table = table;
    }

    public int element() {
        return element;
    }

    public int table() {
        return table;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ElementAndTableInsn && equals((ElementAndTableInsn) obj);
    }

    public boolean equals(ElementAndTableInsn other) {
        return this == other || equals(other) && element == other.element && table == other.table;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(element);
        out.u31(table);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(table).append(' ').append(element);
    }
}
