package com.dylibso.chicory.wasm.types;

public class ElemStart implements Element {
    @Override
    public ElemType getElemType() {
        return ElemType.Start;
    }

    private RefType refType;
    private Instruction[] exprs;

    public ElemStart(RefType refType, Instruction[] exprs) {
        this.refType = refType;
        this.exprs = exprs;
    }

    public RefType getRefType() {
        return refType;
    }

    public void setRefType(RefType refType) {
        this.refType = refType;
    }

    public Instruction[] getExprs() {
        return exprs;
    }

    public void setExprs(Instruction[] exprs) {
        this.exprs = exprs;
    }
}
