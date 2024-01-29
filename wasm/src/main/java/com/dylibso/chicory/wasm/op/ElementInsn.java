package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;

/**
 * An instruction which takes an element as its argument.
 */
public final class ElementInsn extends Insn<Op.Element> implements Cacheable {
    private final int element;

    ElementInsn(Op.Element op, int element) {
        super(op, element);
        this.element = element;
    }

    public int element() {
        return element;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ElementInsn && equals((ElementInsn) obj);
    }

    public boolean equals(ElementInsn other) {
        return this == other || super.equals(other) && element == other.element;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(element);
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(element);
    }
}
