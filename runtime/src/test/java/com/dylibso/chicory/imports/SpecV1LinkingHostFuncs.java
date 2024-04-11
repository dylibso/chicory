package com.dylibso.chicory.imports;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostMemory;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.test.gen.SpecV1LinkingTest;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import java.util.Map;

public class SpecV1LinkingHostFuncs {

    public static HostImports Mf() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
                            "Mf",
                            "call",
                            List.of(),
                            List.of(ValueType.I32))
                });
    }

    public static HostImports Nf() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
                            "Mf",
                            "call",
                            List.of(),
                            List.of(ValueType.I32))
                });
    }

    public static HostImports Mg() {
        // Mg doesn't have imports
        return new HostImports();
    }

    public static HostImports Mt() {
        // Mt doesn't have imports
        return new HostImports();
    }

    public static HostImports Nt() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> {
                                if (args[0].asInt() == 2 || args[0].asInt() == 3) {
                                    return new Value[] {Value.i32(4)};
                                } else {
                                    if (args[0].asInt() < 2) {
                                        throw new WASMRuntimeException("uninitialized element");
                                    } else {
                                        throw new WASMRuntimeException("undefined element");
                                    }
                                }
                            },
                            "Mt",
                            "call",
                            List.of(),
                            List.of(ValueType.I32)),
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(-4)},
                            "Mt",
                            "h",
                            List.of(),
                            List.of(ValueType.I32))
                });
    }

    public static HostImports Ng() {
        var glob = Value.i32(42);
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {glob},
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
                            List.of()),
                },
                new HostGlobal[] {
                    new HostGlobal("Mg", "glob", glob),
                    new HostGlobal(
                            "Mg",
                            "mut_glob",
                            () -> SpecV1LinkingTest.MgInstance.readGlobal(1),
                            v -> SpecV1LinkingTest.MgInstance.writeGlobal(1, v),
                            MutabilityType.Var)
                },
                new HostMemory[] {},
                new HostTable[] {});
    }

    public static HostImports Ot() {
        return new HostImports(
                new HostFunction[] {
                    new HostFunction(
                            (Instance instance, Value... args) -> new Value[] {Value.i32(2)},
                            "Mt",
                            "h",
                            List.of(),
                            List.of(ValueType.I32))
                },
                new HostGlobal[] {},
                new HostMemory[] {},
                new HostTable[] {new HostTable("Mt", "tab", Map.of(1, 1, 2, 2, 3, 3, 10, 10))});
    }

    public static HostImports testModule10() {
        return new HostImports(new HostTable[] {new HostTable("Mt", "tab", Map.of(1, 1, 10, 10))});
    }

    public static HostImports G2() {
        return new HostImports(new HostGlobal[] {new HostGlobal("G1", "g", Value.i32(1))});
    }

    public static HostImports Om() {
        return new HostImports(
                new HostMemory[] {new HostMemory("Mm", "mem", new Memory(new MemoryLimits(1)))});
    }

    public static HostImports testModule18() {
        return new HostImports(
                new HostMemory[] {new HostMemory("Mm", "mem", new Memory(new MemoryLimits(1)))});
    }

    public static HostImports Pm() {
        return new HostImports(
                new HostMemory[] {new HostMemory("Mm", "mem", new Memory(new MemoryLimits(1)))});
    }

    public static HostImports fallback() {
        return new HostImports(
                new HostFunction[] {},
                new HostGlobal[] {
                    // new HostGlobal("spectest", "global_i32", Value.i32(0))},
                },
                new HostMemory[] {
                    //          new HostMemory("spectest", "memory", new Memory(new MemoryLimits(1,
                    // 1)))
                },
                new HostTable[] {});
    }
}
