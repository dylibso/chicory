package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.ValueType;

import java.util.Arrays;
import java.util.Map;

public class HostTable implements FromHost {
    private final String moduleName;
    private final String fieldName;
    private final TableInstance table;

    public HostTable(String moduleName, String fieldName, TableInstance table) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
        this.table = table;
    }

    public HostTable(
            String moduleName,
            String fieldName,
            Map<Integer, Integer> funcRefs) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;

        long maxFuncRef = 0;
        for (var k : funcRefs.keySet()) {
            if (k > maxFuncRef) {
                maxFuncRef = k;
            }
        }

        this.table = new TableInstance(new Table(ValueType.FuncRef, new Limits(maxFuncRef, maxFuncRef)));
        this.table.reset();
    }

    public String moduleName() {
        return moduleName;
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public FromHostType type() {
        return FromHostType.TABLE;
    }

    public TableInstance table() {
        return table;
    }
}
