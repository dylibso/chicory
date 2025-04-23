package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.Map;

public class ImportTable implements ImportValue {
    private final String module;
    private final String name;
    private final TableInstance table;

    public ImportTable(String module, String name, TableInstance table) {
        this.module = module;
        this.name = name;
        this.table = table;
    }

    public ImportTable(String module, String name, Map<Integer, Integer> funcRefs) {
        this.module = module;
        this.name = name;

        long maxFuncRef = 0;
        for (var k : funcRefs.keySet()) {
            if (k > maxFuncRef) {
                maxFuncRef = k;
            }
        }

        this.table =
                new TableInstance(
                        new Table(ValType.FuncRef, new TableLimits(maxFuncRef, maxFuncRef)));
        this.table.reset();
    }

    @Override
    public String module() {
        return module;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ImportValue.Type type() {
        return Type.TABLE;
    }

    public TableInstance table() {
        return table;
    }
}
