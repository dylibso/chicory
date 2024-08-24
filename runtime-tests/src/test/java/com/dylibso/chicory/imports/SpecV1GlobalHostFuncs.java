package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;

public final class SpecV1GlobalHostFuncs {

    private SpecV1GlobalHostFuncs() {}

    public static HostImports fallback() {
        return new HostImports(
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(666))),
                    new HostGlobal("spectest", "global_i64", new GlobalInstance(Value.i64(666))),
                    new HostGlobal("test", "global-i32", new GlobalInstance(Value.i32(0))),
                    new HostGlobal(
                            "test",
                            "global-mut-i32",
                            new GlobalInstance(Value.i32(0)),
                            MutabilityType.Var),
                    new HostGlobal("", "", new GlobalInstance(Value.externRef(0))),
                });
    }
}
