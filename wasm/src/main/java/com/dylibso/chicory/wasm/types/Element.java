package com.dylibso.chicory.wasm.types;

public class Element {
    private long tableIndex;
    private Instruction[] expr;
    private long[] funcIndices;

    public Element(long tableIndex, Instruction[] expr, long[] funcIndices) {
        this.tableIndex = tableIndex;
        this.expr = expr;
        this.funcIndices = funcIndices;
    }

    public long[] getFuncIndices() {
        return funcIndices;
    }

    public Instruction[] getExpr() {
        return expr;
    }

    public long getTableIndex() {
        return tableIndex;
    }
}
