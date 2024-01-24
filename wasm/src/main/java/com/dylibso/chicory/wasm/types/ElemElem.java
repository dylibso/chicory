package com.dylibso.chicory.wasm.types;

public class ElemElem implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Elem;
    }

    private RefType refType;
    private Instruction[][] exprs;

    public ElemElem(RefType refType, Instruction[][] exprs) {
        this.refType = refType;
        this.exprs = exprs;
    }

    public RefType refType() {
        return refType;
    }

    public void setRefType(RefType refType) {
        this.refType = refType;
    }

    public Instruction[][] exprs() {
        return exprs;
    }

    public void setExprs(Instruction[][] exprs) {
        this.exprs = exprs;
    }

    public int size() {
        return exprs.length;
    }
}
