package com.dylibso.chicory.wasm.types;

public class ElemData implements Element {
    @Override
    public ElemType elemType() {
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

    public long tableIndex() {
        return tableIndex;
    }

    public void setTableIndex(long tableIndex) {
        this.tableIndex = tableIndex;
    }

    public Instruction[] exprInstructions() {
        return expr;
    }

    public void setExprInstructions(Instruction[] expr) {
        this.expr = expr;
    }

    public RefType refType() {
        return refType;
    }

    public void setRefType(RefType refType) {
        this.refType = refType;
    }

    public Instruction[][] exprInstructionLists() {
        return exprs;
    }

    public void setExprInstructionLists(Instruction[][] exprs) {
        this.exprs = exprs;
    }

    public int size() {
        return exprs.length;
    }
}
