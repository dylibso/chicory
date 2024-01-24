package com.dylibso.chicory.wasm.types;

public class ElemStart implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Start;
    }

    private RefType refType;
    private Instruction[][] exprs;

    public ElemStart(RefType refType, Instruction[][] exprs) {
        this.refType = refType;
        this.exprs = exprs;
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
