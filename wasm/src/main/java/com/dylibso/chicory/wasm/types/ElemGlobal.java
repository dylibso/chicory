package com.dylibso.chicory.wasm.types;

public class ElemGlobal implements Element {
    @Override
    public ElemType getElemType() {
        return ElemType.Global;
    }

    private Instruction[] expr;
    private Instruction[][] exprs;

    public ElemGlobal(Instruction[] expr, Instruction[][] exprs) {
        this.expr = expr;
        this.exprs = exprs;
    }

    public Instruction[] getExpr() {
        return expr;
    }

    public void setExpr(Instruction[] expr) {
        this.expr = expr;
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
