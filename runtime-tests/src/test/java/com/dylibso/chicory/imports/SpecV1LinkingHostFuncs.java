package com.dylibso.chicory.imports;

import static com.dylibso.chicory.test.gen.SpecV1LinkingTest.MgInstance;
import static com.dylibso.chicory.test.gen.SpecV1LinkingTest.MmInstance;
import static com.dylibso.chicory.test.gen.SpecV1LinkingTest.MsInstance;
import static com.dylibso.chicory.test.gen.SpecV1LinkingTest.MtInstance;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.test.gen.SpecV1LinkingTest;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class SpecV1LinkingHostFuncs {

    private static HostFunction MfCall =
            new HostFunction(
                    (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
                    "Mf",
                    "call",
                    List.of(),
                    List.of(ValueType.I32));

    public static HostImports Mf() {
        return new HostImports(new HostFunction[] {MfCall});
    }

    public static HostImports Nf() {
        return new HostImports(new HostFunction[] {MfCall});
    }

    public static HostImports Mg() {
        return HostImports.empty();
    }

    public static HostImports Mt() {
        return HostImports.empty();
    }

    private static HostFunction Mth() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    return MtInstance.export("h").apply(args);
                },
                "Mt",
                "h",
                List.of(),
                List.of(ValueType.I32));
    }

    private static HostFunction Mtcall() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    return MtInstance.export("call").apply(args);
                },
                "Mt",
                "call",
                List.of(ValueType.I32),
                List.of(ValueType.I32));
    }

    public static HostImports Nt() {
        return new HostImports(new HostFunction[] {Mtcall(), Mth()});
    }

    public static HostImports Ng() {
        return HostImports.builder()
                .addFunction(
                        new HostFunction(
                                (Instance instance, Value... args) ->
                                        new Value[] {SpecV1LinkingTest.MgInstance.readGlobal(0)},
                                "Mg",
                                "get",
                                List.of(),
                                List.of(ValueType.I32)),
                        new HostFunction(
                                (Instance instance, Value... args) ->
                                        new Value[] {SpecV1LinkingTest.MgInstance.readGlobal(1)},
                                "Mg",
                                "get_mut",
                                List.of(),
                                List.of(ValueType.I32)),
                        new HostFunction(
                                (Instance instance, Value... args) -> {
                                    SpecV1LinkingTest.MgInstance.writeGlobal(1, args[0]);
                                    return null;
                                },
                                "Mg",
                                "set_mut",
                                List.of(ValueType.I32),
                                List.of()))
                .addGlobal(
                        new HostGlobal("Mg", "glob", MgInstance.global(0)),
                        new HostGlobal("Mg", "mut_glob", MgInstance.global(1), MutabilityType.Var))
                .build();
    }

    private static HostTable MtTab() {
        return new HostTable("Mt", "tab", MtInstance.table(0));
    }

    public static HostImports Ot() {
        return HostImports.builder().addFunction(Mth()).addTable(MtTab()).build();
    }

    public static HostImports testModule10() {
        return HostImports.builder().addTable(MtTab()).build();
    }

    public static HostImports G2() {
        return HostImports.builder()
                .addGlobal(new HostGlobal("G1", "g", new GlobalInstance(Value.i32(5))))
                .build();
    }

    private static HostMemory MmMem() {
        return new HostMemory("Mm", "mem", MmInstance.memory());
    }

    public static HostImports Om() {
        return new HostImports(new HostMemory[] {MmMem()});
    }

    public static HostImports testModule18() {
        return new HostImports(new HostMemory[] {MmMem()});
    }

    public static HostImports Pm() {
        return new HostImports(new HostMemory[] {MmMem()});
    }

    public static HostImports Nm() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> {
                                return MmInstance.export("load").apply(args);
                            },
                            "Mm",
                            "load",
                            List.of(ValueType.I32),
                            List.of(ValueType.I32))
                });
    }

    public static HostImports Mm() {
        return new HostImports(new HostMemory("Mm", "mem", new Memory(new MemoryLimits(1, 5))));
    }

    public static HostImports Ms() {
        return HostImports.builder()
                .addMemory(new HostMemory("Ms", "memory", new Memory(new MemoryLimits(1, 5))))
                .addTable(
                        new HostTable(
                                "Ms",
                                "table",
                                new TableInstance(new Table(ValueType.FuncRef, new Limits(10)))))
                .build();
    }

    public static HostImports Mref_im() {
        return HostImports.builder()
                .addGlobal(
                        new HostGlobal(
                                "Mref_ex", "g-const-func", new GlobalInstance(Value.funcRef(0))))
                .addGlobal(
                        new HostGlobal(
                                "Mref_ex",
                                "g-const-extern",
                                new GlobalInstance(Value.externRef(0))))
                .addGlobal(
                        new HostGlobal(
                                "Mref_ex",
                                "g-var-func",
                                new GlobalInstance(Value.funcRef(0)),
                                MutabilityType.Var))
                .addGlobal(
                        new HostGlobal(
                                "Mref_ex",
                                "g-var-extern",
                                new GlobalInstance(Value.externRef(0)),
                                MutabilityType.Var))
                .build();
    }

    public static HostImports fallback() {
        var builder =
                HostImports.builder()
                        .addFunction(
                                new HostFunction(
                                        (Instance instance, Value... args) -> {
                                            return null;
                                        },
                                        "spectest",
                                        "print_i32",
                                        List.of(ValueType.I32),
                                        List.of()),
                                new HostFunction(
                                        (Instance instance, Value... args) -> {
                                            return null;
                                        },
                                        "reexport_f",
                                        "print",
                                        List.of(),
                                        List.of()))
                        .addGlobal(
                                new HostGlobal(
                                        "Mref_ex",
                                        "g-const-func",
                                        new GlobalInstance(Value.funcRef(0))))
                        .addGlobal(
                                new HostGlobal(
                                        "Mref_ex",
                                        "g-const-extern",
                                        new GlobalInstance(Value.externRef(0))))
                        .addGlobal(
                                new HostGlobal(
                                        "Mref_ex",
                                        "g-var-func",
                                        new GlobalInstance(Value.funcRef(0)),
                                        MutabilityType.Var))
                        .addGlobal(
                                new HostGlobal(
                                        "Mref_ex",
                                        "g-var-extern",
                                        new GlobalInstance(Value.externRef(0)),
                                        MutabilityType.Var))
                        .addTable(
                                new HostTable(
                                        "Mtable_ex",
                                        "t-func",
                                        new TableInstance(
                                                new Table(ValueType.FuncRef, new Limits(1)))))
                        .addTable(
                                new HostTable(
                                        "Mtable_ex",
                                        "t-extern",
                                        new TableInstance(
                                                new Table(ValueType.ExternRef, new Limits(1)))));

        if (MgInstance != null) {
            builder.addGlobal(
                    new HostGlobal("Mg", "glob", MgInstance.global(0)),
                    new HostGlobal("Mg", "mut_glob", MgInstance.global(1), MutabilityType.Var));
        }

        if (MsInstance != null) {
            builder.addMemory(new HostMemory("Ms", "memory", MsInstance.memory()))
                    .addTable(new HostTable("Ms", "table", MsInstance.table(0)));
        }
        if (MmInstance != null) {
            builder.addMemory(MmMem());
        }
        if (MtInstance != null) {
            builder.addTable(new HostTable("Mt", "tab", MtInstance.table(0)));
        }

        return builder.build();
    }
}
