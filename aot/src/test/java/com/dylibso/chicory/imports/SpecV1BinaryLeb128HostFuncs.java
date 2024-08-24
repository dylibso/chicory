package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public final class SpecV1BinaryLeb128HostFuncs {

    private SpecV1BinaryLeb128HostFuncs() {}

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
                .build();
    }
}
