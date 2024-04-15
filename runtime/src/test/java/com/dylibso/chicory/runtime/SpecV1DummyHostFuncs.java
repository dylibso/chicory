// package com.dylibso.chicory.runtime;
//
// import static com.dylibso.chicory.runtime.DummyTest.MtInstance;
//
// import com.dylibso.chicory.wasm.types.Limits;
// import com.dylibso.chicory.wasm.types.MemoryLimits;
// import com.dylibso.chicory.wasm.types.MutabilityType;
// import com.dylibso.chicory.wasm.types.Table;
// import com.dylibso.chicory.wasm.types.Value;
// import com.dylibso.chicory.wasm.types.ValueType;
// import java.util.List;
//
// public class SpecV1DummyHostFuncs {
//
//    public static HostImports Mf() {
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
//                            "Mf",
//                            "call",
//                            List.of(),
//                            List.of(ValueType.I32))
//                });
//    }
//
//    public static HostImports Nf() {
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
//                            "Mf",
//                            "call",
//                            List.of(),
//                            List.of(ValueType.I32))
//                });
//    }
//
//    public static HostImports Mg() {
//        // Mg doesn't have imports
//        return new HostImports();
//    }
//
//    public static HostImports Mt() {
//        // Mt doesn't have imports
//        return new HostImports();
//    }
//
//    private static ExportFunction Mth() {
//        return MtInstance.export("h");
//    }
//
//    private static ExportFunction Mtcall() {
//        return MtInstance.export("call");
//    }
//
//    public static HostImports Nt() {
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> {
//                                System.out.println("Nt instance: Mt.call " + args[0]);
//                                return Mtcall().apply(args);
//                            },
//                            "Mt",
//                            "call",
//                            List.of(ValueType.I32),
//                            List.of(ValueType.I32)),
//                    new HostFunction(
//                            (Instance instance, Value... args) -> {
//                                System.out.println("Nt instance: Mt.h");
//                                return Mth().apply(args);
//                            },
//                            "Mt",
//                            "h",
//                            List.of(),
//                            List.of(ValueType.I32))
//                });
//    }
//
//    public static HostImports Ng() {
//        var glob = Value.i32(42);
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> new Value[] {glob},
//                            "Mg",
//                            "get",
//                            List.of(),
//                            List.of(ValueType.I32)),
//                    new HostFunction(
//                            (Instance instance, Value... args) ->
//                                    new Value[] {DummyTest.MgInstance.readGlobal(1)},
//                            "Mg",
//                            "get_mut",
//                            List.of(),
//                            List.of(ValueType.I32)),
//                    new HostFunction(
//                            (Instance instance, Value... args) -> {
//                                DummyTest.MgInstance.writeGlobal(1, args[0]);
//                                return null;
//                            },
//                            "Mg",
//                            "set_mut",
//                            List.of(ValueType.I32),
//                            List.of()),
//                },
//                new HostGlobal[] {
//                    new HostGlobal("Mg", "glob", glob),
//                    new HostGlobal(
//                            "Mg",
//                            "mut_glob",
//                            () -> DummyTest.MgInstance.readGlobal(1),
//                            v -> DummyTest.MgInstance.writeGlobal(1, v),
//                            MutabilityType.Var)
//                },
//                new HostMemory[] {},
//                new HostTable[] {});
//    }
//
//    private static HostTable MtTable() {
//        return new HostTable("Mt", "tab", MtInstance.table(0));
//    }
//
//    public static HostImports Ot() {
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> {
//                                System.out.println("Ot instance: Mt.h");
//                                // MtInstance.table(0).setRef(2, 1);
//                                return Mth().apply(args);
//                            },
//                            "Mt",
//                            "h",
//                            List.of(ValueType.I32),
//                            List.of(ValueType.I32))
//                },
//                new HostGlobal[] {},
//                new HostMemory[] {},
//                new HostTable[] {
//                    new HostTable("Mt", "tab", new Table(ValueType.FuncRef, new Limits(10)))
//                });
//        // new HostTable[] {MtTable()});
//    }
//
//    public static HostImports testModule10() {
//        return new HostImports(new HostTable[] {MtTable()});
//    }
//
//    public static HostImports G2() {
//        return new HostImports(new HostGlobal[] {new HostGlobal("G1", "g", Value.i32(5))});
//    }
//
//    private static HostMemory MmMem =
//            new HostMemory("Mm", "mem", new Memory(new MemoryLimits(1, 5)));
//
//    public static HostImports Om() {
//        return new HostImports(new HostMemory[] {MmMem});
//    }
//
//    public static HostImports testModule18() {
//        return new HostImports(new HostMemory[] {MmMem});
//    }
//
//    public static HostImports Pm() {
//        return new HostImports(new HostMemory[] {MmMem});
//    }
//
//    public static HostImports Nm() {
//        return new HostImports(
//                new HostFunction[] {
//                    new HostFunction(
//                            (Instance instance, Value... args) -> {
//                                System.out.println("Nm instance: Mm.load");
//                                return new Value[] {Value.i32(2)};
//                            },
//                            "Mm",
//                            "load",
//                            List.of(ValueType.I32),
//                            List.of(ValueType.I32))
//                });
//    }
//
//    public static HostImports fallback() {
//        return new HostImports(
//                new HostFunction[] {},
//                new HostGlobal[] {
//                    // new HostGlobal("spectest", "global_i32", Value.i32(0))},
//                },
//                new HostMemory[] {
//                    //          new HostMemory("spectest", "memory", new Memory(new
// MemoryLimits(1,
//                    // 1)))
//                },
//                new HostTable[] {});
//    }
// }
