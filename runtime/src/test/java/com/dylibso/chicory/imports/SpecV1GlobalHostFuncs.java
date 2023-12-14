package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.wasm.types.Value;

public class SpecV1GlobalHostFuncs {

    public static HostImports fallback() {
        return new HostImports(
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", Value.i32(666)),
                    new HostGlobal("spectest", "global_i64", Value.i64(666))
                });
    }
}
