package com.dylibso.chicory.wasm.types;

public class ElemFunc implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Func;
    }

    private long[] funcIndices;

    public ElemFunc(long[] funcIndices) {
        this.funcIndices = funcIndices;
    }

    public long[] funcIndices() {
        return funcIndices;
    }

    public void setFuncIndices(long[] funcIndices) {
        this.funcIndices = funcIndices;
    }

    public int size() {
        return funcIndices.length;
    }
}
