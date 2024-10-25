package com.dylibso.chicory.wasm.types;

import java.util.Objects;

public class Table {
    private final ValueType elementType;
    private final TableLimits limits;

    public Table(ValueType elementType, TableLimits limits) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ValueType elementType() {
        return elementType;
    }

    public TableLimits limits() {
        return limits;
    }
}
