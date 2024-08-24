package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public final class SpecV1ExportsHostFuncs {

    private SpecV1ExportsHostFuncs() {}

    public static HostImports fallback() {
        return HostImports.builder()
                .addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    return null;
                                },
                                "spectest",
                                "print_i32",
                                List.of(ValueType.I32),
                                List.of()))
                .addTable(
                        new HostTable(
                                "spectest",
                                "table",
                                new TableInstance(
                                        new Table(ValueType.FuncRef, new Limits(10, 20)))))
                .addMemory(new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 2))))
                .addGlobal(
                        new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(0))))
                .build();
    }
}
