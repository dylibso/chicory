package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.*;
import com.dylibso.chicory.wasm.types.MemoryLimits;

public class SpecV1DataHostFuncs {

    public static HostImports fallback() {

        return new HostImports(
                new HostFunction[] {},
                new HostGlobal[] {},
                new HostMemory[] {
                    new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 1)))
                },
                new HostTable[] {});
    }
}
