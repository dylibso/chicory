package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.Arrays;

public class Table {
    private final ElementType elementType;
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

    public ElementType elementType() {
        return elementType;
    }

    public long limitMin() {
        return limitMin;
    }

    public Long limitMax() {
        return limitMax;
    }

    public int size() {
        return refs.length;
    }

    public int grow(int size, Integer value) {
        var oldSize = refs.length;
        var targetSize = oldSize + size;
        if (size < 0 || (limitMax != 0 && targetSize > limitMax)) {
            return -1;
        }
        var newRefs = Arrays.copyOf(refs, targetSize);
        Arrays.fill(newRefs, oldSize, targetSize, value);
        refs = newRefs;
        return oldSize;
    }

    public Value ref(int index) {
        int res = REF_NULL_VALUE;
        try {
            res = this.refs[index];
        } catch (IndexOutOfBoundsException e) {
            throw new ChicoryException("undefined element", e);
        }
        if (this.elementType() == ElementType.FuncRef) {
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
