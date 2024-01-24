package com.dylibso.chicory.wasm.types;

public class ElemType implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Type;
    }

    private Instruction[] expr;
    private long[] funcIndices;

    public ElemType(Instruction[] expr, long[] funcIndices) {
        this.expr = expr;
        this.funcIndices = funcIndices;
    }

    public long[] funcIndices() {
        return funcIndices;
    }

    public Instruction[] exprInstructions() {
        return expr;
    }

    public int size() {
        return expr.length;
    }
}
