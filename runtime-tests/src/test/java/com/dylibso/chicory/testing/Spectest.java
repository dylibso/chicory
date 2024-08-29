package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.WasmFunctionHandle;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

// https://github.com/WebAssembly/spec/blob/ee82c8e50c5106e0cedada0a083d4cc4129034a2/interpreter/host/spectest.ml
public final class Spectest {
    private static final WasmFunctionHandle noop = (Instance instance, Value... args) -> null;

    private Spectest() {}

    public static HostImports toHostImports() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(noop, "spectest", "print", List.of(), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i32", List.of(ValueType.I32), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i32_1", List.of(ValueType.I32), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i32_2", List.of(ValueType.I32), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_f32", List.of(ValueType.F32), List.of()),
                    new HostFunction(
                            noop,
                            "spectest",
                            "print_i32_f32",
                            List.of(ValueType.I32, ValueType.F32),
                            List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i64", List.of(ValueType.I64), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i64_1", List.of(ValueType.I64), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_i64_2", List.of(ValueType.I64), List.of()),
                    new HostFunction(
                            noop, "spectest", "print_f64", List.of(ValueType.F64), List.of()),
                    new HostFunction(
                            noop,
                            "spectest",
                            "print_f64_f64",
                            List.of(ValueType.F64, ValueType.F64),
                            List.of())
                },
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(666))),
                    new HostGlobal("spectest", "global_i64", new GlobalInstance(Value.i64(666))),
                    new HostGlobal(
                            "spectest", "global_f32", new GlobalInstance(Value.fromFloat(666.6f))),
                    new HostGlobal(
                            "spectest", "global_f64", new GlobalInstance(Value.fromDouble(666.6))),
                },
                new HostMemory[] {
                    new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 2)))
                },
                new HostTable[] {
                    new HostTable(
                            "spectest",
                            "table",
                            new TableInstance(new Table(ValueType.FuncRef, new Limits(10, 20))))
                });
    }
}
