package com.dylibso.chicory.wasm.types;

public class ElemMem implements Element {
    @Override
    public ElemType elemType() {
        return ElemType.Mem;
    }

    private long[] funcIndices;

    public ElemMem(long[] funcIndices) {
        this.funcIndices = funcIndices;
    }

    public int size() {
        return funcIndices.length;
    }
}
