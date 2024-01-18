package com.dylibso.chicory.wasm.types;

public class ElemData implements Element {
    @Override
    public ElemType getElemType() {
        return ElemType.Data;
    }

    private long tableIndex;
    private Instruction[] expr;
    private RefType refType;
    private Instruction[][] exprs;

    public ElemData(long tableIndex, Instruction[] expr, RefType refType, Instruction[][] exprs) {
        this.tableIndex = tableIndex;
        this.expr = expr;
        this.refType = refType;
        this.exprs = exprs;
    }

    public long getTableIndex() {
        return tableIndex;
    }

    public void setTableIndex(long tableIndex) {
        this.tableIndex = tableIndex;
    }

    public Instruction[] getExpr() {
        return expr;
    }

    public void setExpr(Instruction[] expr) {
        this.expr = expr;
    }

    public RefType getRefType() {
        return refType;
    }

    public void setRefType(RefType refType) {
        this.refType = refType;
    }

    public Instruction[][] getExprs() {
        return exprs;
    }

    public void setExprs(Instruction[][] exprs) {
        this.exprs = exprs;
    }

    public int getSize() {
        return exprs.length;
    }
}
