package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ElementType;
import com.dylibso.chicory.wasm.types.Table;
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

        this.table = new Table(ElementType.FuncRef, maxFuncRef, maxFuncRef);

        for (int i = 0; i < maxFuncRef; i++) {
            if (funcRefs.containsKey(i)) {
                this.table.setRef(i, funcRefs.get(i));
            } else {
                this.table.setRef(i, Table.UNINITIALIZED);
            }
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public FromHostType getType() {
        return FromHostType.TABLE;
    }

    public Table getTable() {
        return table;
    }
}
