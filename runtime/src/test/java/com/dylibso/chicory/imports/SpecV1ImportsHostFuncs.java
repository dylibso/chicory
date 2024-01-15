package com.dylibso.chicory.imports;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpecV1ImportsHostFuncs {

    private static HostImports base() {
        var printI32 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32",
                        List.of(ValueType.I32),
                        List.of());
        var printI32_1 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_1",
                        List.of(ValueType.I32),
                        List.of());
        var printI32_2 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_2",
                        List.of(ValueType.I32),
                        List.of());
        var printF32 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f32",
                        List.of(ValueType.F32),
                        List.of());
        var printI32F32 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_f32",
                        List.of(ValueType.I32, ValueType.F32),
                        List.of());
        var printI64 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64",
                        List.of(ValueType.I64),
                        List.of());
        var printI64_1 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64_1",
                        List.of(ValueType.I64),
                        List.of());
        var printI64_2 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64_2",
                        List.of(ValueType.I64),
                        List.of());
        var printF64 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f64",
                        List.of(ValueType.F64),
                        List.of());
        var printF64_1 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f64_1",
                        List.of(ValueType.F64),
                        List.of());
        var printF64F64 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f64_f64",
                        List.of(ValueType.F64, ValueType.F64),
                        List.of());
        var table =
                new HostTable(
                        "spectest",
                        "table",
                        Map.of(1, 1, 2, 2, 10, 10, 24, 24, 100, REF_NULL_VALUE));
        var mem = new Memory(new MemoryLimits(1, 2));
        mem.writeI32(10, 16); // data d_a(offset: 10) = "\10";
        var memory = new HostMemory("spectest", "memory", mem);
        return new HostImports(
                new HostFunction[] {
                    printI32,
                    printI32_1,
                    printI32_2,
                    printF32,
                    printI32F32,
                    printI64,
                    printI64_1,
                    printI64_2,
                    printF64,
                    printF64F64
                },
                new HostGlobal[] {},
                memory,
                new HostTable[] {table});
    }

    public static HostImports testModule11() {
        return new HostImports(
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", Value.i32(666)),
                    new HostGlobal("spectest", "global_i32_1", Value.i32(666)),
                    new HostGlobal("spectest", "global_i32_2", Value.i32(666)),
                    new HostGlobal("spectest", "global_i32_3", Value.i32(666)),
                    new HostGlobal("spectest", "global_i64", Value.i64(666)),
                    new HostGlobal("spectest", "global_f32", Value.f32(1)),
                    new HostGlobal("spectest", "global_f64", Value.f64(1)),
                });
    }

    private static HostImports memory2Inf =
            new HostImports(
                    new HostMemory(
                            "test", "memory-2-inf", new Memory(MemoryLimits.defaultLimits())));

    public static HostImports testModule40() {
        return memory2Inf;
    }

    public static HostImports testModule41() {
        return memory2Inf;
    }

    public static HostImports testModule42() {
        return memory2Inf;
    }

    public static HostImports Mgim1() {
        var mem = new Memory(new MemoryLimits(2, 3));
        return new HostImports(new HostMemory("grown-memory", "memory", mem));
    }

    public static HostImports Mgim2() {
        var mem = new Memory(new MemoryLimits(3, 4));
        return new HostImports(new HostMemory("grown-imported-memory", "memory", mem));
    }

    public static HostImports fallback() {
        var testFunc =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));
        var testFuncI64 =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            return new Value[] {args[0]};
                        },
                        "test",
                        "func-i64->i64",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));
        var base = base().getFunctions();
        var additional = new HostFunction[] {testFunc, testFuncI64};
        HostFunction[] hostFunctions = Arrays.copyOf(base, base.length + additional.length);
        System.arraycopy(additional, 0, hostFunctions, base.length, additional.length);
        return new HostImports(
                hostFunctions, new HostGlobal[] {}, base().getMemories()[0], base().getTables());
    }
}
