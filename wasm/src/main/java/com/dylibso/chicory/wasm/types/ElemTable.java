package com.dylibso.chicory.wasm.types;

public class ElemTable implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Table;
    }

    private long tableIndex;
    private Instruction[] expr;
    private long[] funcIndices;

    public ElemTable(long tableIndex, Instruction[] expr, long[] funcIndices) {
        this.tableIndex = tableIndex;
        this.expr = expr;
        this.funcIndices = funcIndices;
    }

    public long[] funcIndices() {
        return funcIndices;
    }

    public Instruction[] exprInstructions() {
        return expr;
    }

    public long tableIndex() {
        return tableIndex;
    }

    public int size() {
        return expr.length;
    }
}
