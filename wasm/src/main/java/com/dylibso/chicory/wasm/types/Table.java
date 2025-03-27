package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

public class Table {
    private final ValueType elementType;
    private final TableLimits limits;

    private final List<Instruction> init;

    public Table(ValueType elementType, TableLimits limits) {
        this(
                elementType,
                limits,
                List.of(new Instruction(-1, OpCode.REF_NULL, new long[] {elementType.operand()})));
    }

    public Table(ValueType elementType, TableLimits limits, List<Instruction> init) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
        this.init = init;
    }

    public ValueType elementType() {
        return elementType;
    }

    public TableLimits limits() {
        return limits;
    }

    public List<Instruction> initialize() {
        return init;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Table)) {
            return false;
        }
        Table table = (Table) o;
        return elementType.equals(table.elementType) && Objects.equals(limits, table.limits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, limits);
    }
}
