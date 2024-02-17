package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Map;

public class HostTable implements FromHost {
    private final String moduleName;
    private final String fieldName;
    private final Table table;

    public HostTable(String moduleName, String fieldName, Table table) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
        this.table = table;
    }

    public HostTable(String moduleName, String fieldName, Map<Integer, Integer> funcRefs) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;

        long maxFuncRef = 0;
        for (var k : funcRefs.keySet()) {
            if (k > maxFuncRef) {
                maxFuncRef = k;
            }
        }

        this.table = new Table(ValueType.FuncRef, new Limits(maxFuncRef, maxFuncRef));

        for (int i = 0; i < maxFuncRef; i++) {
            this.table.setRef(i, funcRefs.getOrDefault(i, REF_NULL_VALUE));
        }
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

    public Table table() {
        return table;
    }
}
