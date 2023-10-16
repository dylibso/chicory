package com.dylibso.chicory.wasm.types;

public class Table {
    private ElementType elementType;
    private long limitMin;
    private Long limitMax;

    public Table(ElementType elementType, long limitMin, Long limitMax) {
        this.elementType = elementType;
        this.limitMin = limitMin;
        this.limitMax = limitMax;
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
}
