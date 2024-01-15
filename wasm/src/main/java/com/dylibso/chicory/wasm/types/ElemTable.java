package com.dylibso.chicory.wasm.types;

public class ElemTable implements Element {
    @Override
    public ElemType getElemType() {
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
