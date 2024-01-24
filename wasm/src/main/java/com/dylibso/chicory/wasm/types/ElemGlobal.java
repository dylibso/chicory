package com.dylibso.chicory.wasm.types;

public class ElemGlobal implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Global;
    }

    private Instruction[] expr;
    private Instruction[][] exprs;

    public ElemGlobal(Instruction[] expr, Instruction[][] exprs) {
        this.expr = expr;
        this.exprs = exprs;
    }

    public Instruction[] exprInstructions() {
        return expr;
    }

    public void setExprInstructions(Instruction[] expr) {
        this.expr = expr;
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
