package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class SpecV1ImportsHostFuncs {

    public static HostImports testModule11() {
        return HostImports.builder()
                .addGlobal(
                        new HostGlobal(
                                "spectest", "global_i32", new GlobalInstance(Value.i32(666))),
                        new HostGlobal(
                                "spectest", "global_i32_1", new GlobalInstance(Value.i32(666))),
                        new HostGlobal(
                                "spectest", "global_i32_2", new GlobalInstance(Value.i32(666))),
                        new HostGlobal(
                                "spectest", "global_i32_3", new GlobalInstance(Value.i32(666))),
                        new HostGlobal(
                                "spectest", "global_i64", new GlobalInstance(Value.i64(666))),
                        new HostGlobal("spectest", "global_f32", new GlobalInstance(Value.f32(1))),
                        new HostGlobal("spectest", "global_f64", new GlobalInstance(Value.f64(1))))
                .build();
    }

    // similar to:
    // https://github.com/tetratelabs/wazero/blob/c345ddf2b5d65f69ee4daa1bb49fe896d1306778/internal/integration_test/spectest/v1/testdata/imports.wast#L417-L419
    public static HostImports testModule40() {
        return new HostImports(
                new HostMemory("test", "memory-2-inf", new Memory(new MemoryLimits(2))));
    }

    public static HostImports testModule41() {
        return new HostImports(
                new HostMemory("test", "memory-2-inf", new Memory(new MemoryLimits(1))));
    }

    public static HostImports testModule42() {
        return new HostImports(
                new HostMemory("test", "memory-2-inf", new Memory(new MemoryLimits(0))));
    }

    public static HostImports testModule49() {
        return new HostImports(
                new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1, 2))));
    }

    public static HostImports Mgim1() {
        return new HostImports(
                new HostMemory("grown-memory", "memory", new Memory(new MemoryLimits(2))));
    }

    public static HostImports Mgim2() {
        return new HostImports(
                new HostMemory("grown-imported-memory", "memory", new Memory(new MemoryLimits(3))));
    }

    public static HostImports fallback() {
        var testFunc =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func",
                        List.of(),
                        List.of());
        var testFuncI32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func-i32",
                        List.of(ValueType.I32),
                        List.of());
        var testFuncToI32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func->i32",
                        List.of(),
                        List.of(ValueType.I32));
        var testFuncI32ToI32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func-i32->i32",
                        List.of(ValueType.I32),
                        List.of(ValueType.I32));
        var testFuncI64 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return new Value[] {args[0]};
                        },
                        "test",
                        "func-i64->i64",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));
        var testFuncF32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func-f32",
                        List.of(ValueType.F32),
                        List.of());
        var testFuncToF32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "test",
                        "func->f32",
                        List.of(),
                        List.of(ValueType.F32));
        var printI32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32",
                        List.of(ValueType.I32),
                        List.of());
        var printI32_1 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_1",
                        List.of(ValueType.I32),
                        List.of());
        var printI32_2 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_2",
                        List.of(ValueType.I32),
                        List.of());
        var printF32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f32",
                        List.of(ValueType.F32),
                        List.of());
        var printI32F32 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i32_f32",
                        List.of(ValueType.I32, ValueType.F32),
                        List.of());
        var printI64 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64",
                        List.of(ValueType.I64),
                        List.of());
        var printI64_1 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64_1",
                        List.of(ValueType.I64),
                        List.of());
        var printI64_2 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_i64_2",
                        List.of(ValueType.I64),
                        List.of());
        var printF64 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            return null;
                        },
                        "spectest",
                        "print_f64",
                        List.of(ValueType.F64),
                        List.of());
        var printF64F64 =
                new HostFunction(
                        (Instance instance, Value... args) -> {
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
                        new TableInstance(new Table(ValueType.FuncRef, new Limits(10, 20))));
        var table10Inf =
                new HostTable(
                        "test",
                        "table-10-inf",
                        new TableInstance(new Table(ValueType.FuncRef, new Limits(10))));
        var table1020 =
                new HostTable(
                        "test",
                        "table-10-20",
                        new TableInstance(new Table(ValueType.FuncRef, new Limits(10, 20))));

        return HostImports.builder()
                .addFunction(
                        printI32,
                        printI32_1,
                        printI32_2,
                        printF32,
                        printI32F32,
                        printI64,
                        printI64_1,
                        printI64_2,
                        printF64,
                        printF64F64,
                        testFunc,
                        testFuncI32,
                        testFuncToI32,
                        testFuncI32ToI32,
                        testFuncI64,
                        testFuncF32,
                        testFuncToF32)
                .addGlobal(
                        new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(0))),
                        new HostGlobal("test", "global-i32", new GlobalInstance(Value.i32(0))),
                        new HostGlobal("test", "global-f32", new GlobalInstance(Value.f32(0))),
                        new HostGlobal(
                                "test",
                                "global-mut-i64",
                                new GlobalInstance(Value.i64(0)),
                                MutabilityType.Var))
                .addMemory(
                        new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1))),
                        new HostMemory(
                                "test", "memory-2-inf", new Memory(MemoryLimits.defaultLimits())))
                .addTable(table, table10Inf, table1020)
                .build();
    }
}
