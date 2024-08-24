package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public final class SpecV1NamesHostFuncs {

    private SpecV1NamesHostFuncs() {}

    public static HostImports fallback() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> {
                                return null;
                            },
                            "spectest",
                            "print_i32",
                            List.of(ValueType.I32),
                            List.of())
                });
    }
}
