package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public final class SpecV1TableInitHostFuncs {

    private SpecV1TableInitHostFuncs() {}

    public static HostImports fallback() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(0)},
                            "a",
                            "ef0",
                            List.of(),
                            List.of(ValueType.I32)),
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(1)},
                            "a",
                            "ef1",
                            List.of(),
                            List.of(ValueType.I32)),
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
                            "a",
                            "ef2",
                            List.of(),
                            List.of(ValueType.I32)),
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(3)},
                            "a",
                            "ef3",
                            List.of(),
                            List.of(ValueType.I32)),
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(4)},
                            "a",
                            "ef4",
                            List.of(),
                            List.of(ValueType.I32))
                });
    }
}
