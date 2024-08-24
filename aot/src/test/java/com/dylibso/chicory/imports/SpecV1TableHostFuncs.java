package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.ValueType;

public final class SpecV1TableHostFuncs {

    private SpecV1TableHostFuncs() {}

    public static HostImports fallback() {
        return HostImports.builder()
                .addTable(
                        new HostTable(
                                "spectest",
                                "table",
                                new TableInstance(new Table(ValueType.FuncRef, new Limits(10)))))
                .build();
    }
}
