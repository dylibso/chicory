package com.dylibso.chicory.wasm.types;

public class ElemType implements Element {
    @Override
    public ElemType getElemType() {
        return ElemType.Type;
    }

    private Instruction expr;
    private long[] funcIndices;

    public ElemType(Instruction expr, long[] funcIndices) {
        this.expr = expr;
        this.funcIndices = funcIndices;
    }

    public long[] getFuncIndices() {
        return funcIndices;
    }

    public Instruction getExpr() {
        return expr;
    }
}
