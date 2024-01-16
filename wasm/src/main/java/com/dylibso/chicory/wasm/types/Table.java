package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

public class Table {
    private ElementType elementType;
    private long limitMin;
    private long limitMax;

    private int[] refs;

    public Table(ElementType elementType, long limitMin, Long limitMax) {
        this.elementType = elementType;
        this.limitMin = limitMin;
        if (limitMax != null) {
            this.limitMax = limitMax;
        }
        refs = new int[(int) limitMin];
        for (int i = 0; i < limitMin; i++) {
            refs[i] = REF_NULL_VALUE;
        }
    }

    public ElementType getElementType() {
        return elementType;
    }

    public long getLimitMin() {
        return limitMin;
    }

    public Long getLimitMax() {
        return limitMax;
    }

    public Value getRef(int index) {
        int res = REF_NULL_VALUE;
        try {
            res = this.refs[index];
        } catch (IndexOutOfBoundsException e) {
            throw new ChicoryException("undefined element", e);
        }
        if (this.getElementType() == ElementType.FuncRef) {
            return Value.funcRef(res);
        } else {
            return Value.externRef(res);
        }
    }

    public void setRef(int index, Integer value) {
        try {
            this.refs[index] = value;
        } catch (IndexOutOfBoundsException e) {
            throw new ChicoryException("out of bounds table access", e);
        }
    }
}
