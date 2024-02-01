package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Value;

public class SpecV1DataHostFuncs {
    public static HostImports fallback() {

        return new HostImports(
                new HostFunction[] {},
                new HostGlobal[] {new HostGlobal("spectest", "global_i32", Value.i32(0))},
                new HostMemory[] {
                    new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 1)))
                },
                new HostTable[] {});
    }
}
