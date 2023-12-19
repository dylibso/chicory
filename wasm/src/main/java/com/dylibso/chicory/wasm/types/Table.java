package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.ArrayList;
import java.util.List;

public class Table {
    private ElementType elementType;
    private long limitMin;
    private long limitMax;
    private List<Integer> funcRefs;

    public Table(ElementType elementType, long limitMin, Long limitMax) {
        this.elementType = elementType;
        this.limitMin = limitMin;
        if (limitMax != null) {
            this.limitMax = limitMax;
        }
        this.funcRefs = new ArrayList<>();
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

    public void addFuncRef(Integer funcRef) {
        this.funcRefs.add(funcRef);
    }

    public int getFuncRef(int index) {
        var res = this.funcRefs.get(index);
        if (res == null) {
            throw new ChicoryException("uninitialized element");
        }
        return res;
    }
}
