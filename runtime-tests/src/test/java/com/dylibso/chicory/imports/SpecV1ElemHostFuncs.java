package com.dylibso.chicory.imports;

import static com.dylibso.chicory.test.gen.SpecV1ElemTest.mInstance;
import static com.dylibso.chicory.test.gen.SpecV1ElemTest.module1Instance;
import static com.dylibso.chicory.test.gen.SpecV1ElemTest.module4Instance;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public final class SpecV1ElemHostFuncs {

    private static GlobalInstance glob = new GlobalInstance(Value.i32(123));

    private SpecV1ElemHostFuncs() {}

    public static HostImports fallback() {
        return HostImports.builder()
                .addGlobal(
                        new HostGlobal("test", "global-i32", new GlobalInstance(Value.i32(0))),
                        new HostGlobal("spectest", "global_i32", glob),
                        new HostGlobal(
                                "test",
                                "global-mut-i32",
                                new GlobalInstance(Value.i32(0)),
                                MutabilityType.Var),
                        new HostGlobal(
                                "test", "g", new GlobalInstance(Value.i32(0)), MutabilityType.Var))
                .addTable(
                        new HostTable(
                                "spectest",
                                "table",
                                new TableInstance(new Table(ValueType.FuncRef, new Limits(10)))))
                .build();
    }

    public static HostImports testModule17() {
        return new HostImports(
                new HostTable[] {
                    new HostTable(
                            "spectest",
                            "table",
                            new TableInstance(new Table(ValueType.FuncRef, new Limits(1, 100))))
                });
    }

    public static HostImports testModule19() {
        return new HostImports(
                new HostTable[] {
                    new HostTable(
                            "spectest",
                            "table",
                            new TableInstance(new Table(ValueType.FuncRef, new Limits(10, 30))))
                });
    }

    private static HostTable module1SharedTable() {
        return new HostTable("module1", "shared-table", module1Instance.table(0));
    }

    public static HostImports module2() {
        return new HostImports(new HostTable[] {module1SharedTable()});
    }

    public static HostImports module3() {
        return new HostImports(new HostTable[] {module1SharedTable()});
    }

    public static HostImports testModule28() {
        return new HostImports(
                new HostTable[] {new HostTable("exporter", "table", mInstance.table(0))});
    }

    public static HostImports testModule30() {
        return new HostImports(
                new HostGlobal[] {new HostGlobal("module4", "f", module4Instance.global(0))});
    }
}
