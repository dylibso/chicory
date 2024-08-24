package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;

public final class SpecV1DataHostFuncs {

    private SpecV1DataHostFuncs() {}

    public static HostImports fallback() {

        return new HostImports(
                new HostFunction[] {},
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(666))),
                    new HostGlobal("spectest", "global_i64", new GlobalInstance(Value.i64(666))),
                    new HostGlobal("test", "global-i32", new GlobalInstance(Value.i32(0))),
                    new HostGlobal(
                            "test",
                            "global-mut-i32",
                            new GlobalInstance(Value.i32(0)),
                            MutabilityType.Var),
                    new HostGlobal(
                            "test", "g", new GlobalInstance(Value.i32(0)), MutabilityType.Var)
                },
                new HostMemory[] {
                    new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 1)))
                },
                new HostTable[] {});
    }
}
