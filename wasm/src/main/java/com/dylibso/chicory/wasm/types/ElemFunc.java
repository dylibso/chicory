package com.dylibso.chicory.wasm.types;

public class ElemFunc implements Element {
    @Override
    public ElemType getElemType() {
        return ElemType.Func;
    }

    private long[] funcIndices;

    public ElemFunc(long[] funcIndices) {
        this.funcIndices = funcIndices;
    }

    public long[] getFuncIndices() {
        return funcIndices;
    }

    public void setFuncIndices(long[] funcIndices) {
        this.funcIndices = funcIndices;
    }

    public int getSize() {
        return funcIndices.length;
    }
}
