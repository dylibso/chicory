package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

public class Table {
    private final ValType elementType;
    private final TableLimits limits;

    public Table(ValType elementType, TableLimits limits) {
        this(
                elementType,
                limits,
                List.of(new Instruction(-1, OpCode.REF_NULL, new long[] {elementType.typeIdx()})));
    }

    @Deprecated(since = "23/05/2025", forRemoval = true)
    public Table(ValueType elementType, TableLimits limits) {
        this(
                elementType.toNew(),
                limits,
                List.of(
                        new Instruction(
                                -1, OpCode.REF_NULL, new long[] {elementType.toNew().typeIdx()})));
    }

    public Table(ValType elementType, TableLimits limits, List<Instruction> init) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Deprecated(since = "23/05/2025", forRemoval = true)
    public Table(ValueType elementType, TableLimits limits, List<Instruction> init) {
        this.elementType = Objects.requireNonNull(elementType, "elementType").toNew();
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ValType elementType() {
        return elementType;
    }

    public TableLimits limits() {
        return limits;
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
