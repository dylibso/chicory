package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Map;

public class ExternalTable implements ExternalValue {
    private final String moduleName;
    private final String symbolName;
    private final TableInstance table;

    public ExternalTable(String moduleName, String symbolName, TableInstance table) {
        this.moduleName = moduleName;
        this.symbolName = symbolName;
        this.table = table;
    }

    public ExternalTable(String moduleName, String symbolName, Map<Integer, Integer> funcRefs) {
        this.moduleName = moduleName;
        this.symbolName = symbolName;

        long maxFuncRef = 0;
        for (var k : funcRefs.keySet()) {
            if (k > maxFuncRef) {
                maxFuncRef = k;
            }
        }

        this.table =
                new TableInstance(new Table(ValueType.FuncRef, new Limits(maxFuncRef, maxFuncRef)));
        this.table.reset();
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public String symbolName() {
        return symbolName;
    }

    @Override
    public ExternalValue.Type type() {
        return Type.TABLE;
    }

    public TableInstance table() {
        return table;
    }
}
