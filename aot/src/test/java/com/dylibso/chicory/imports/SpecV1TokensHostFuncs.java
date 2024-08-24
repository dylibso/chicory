package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public final class SpecV1TokensHostFuncs {

    private SpecV1TokensHostFuncs() {}

    public static HostImports fallback() {
        return HostImports.builder()
                .addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    return null;
                                },
                                "spectest",
                                "print",
                                List.of(),
                                List.of()))
                .build();
    }
}
