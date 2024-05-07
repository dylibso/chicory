package com.dylibso.chicory.imports;

import static com.dylibso.chicory.test.gen.SpecV1ElemTest.*;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostGlobal;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.HostTable;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

public class SpecV1ElemHostFuncs {

    public static HostImports fallback() {
        return new HostImports();
    }

    public static HostImports testModule3() {
        return new HostImports(
                new HostTable[] {
                    new HostTable(
                            "spectest",
                            "table",
                            new TableInstance(new Table(ValueType.FuncRef, new Limits(1))))
                });
    }

    public static HostImports testModule5() {
        return new HostImports(
                new HostTable[] {
                    new HostTable(
                            "spectest",
                            "table",
                            new TableInstance(new Table(ValueType.FuncRef, new Limits(10))))
                });
    }

    public static HostImports testModule6() {
        return new HostImports(
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(123)))
                });
    }

    public static HostImports testModule7() {
        return new HostImports(
                new HostGlobal[] {
                    new HostGlobal("spectest", "global_i32", new GlobalInstance(Value.i32(321)))
                });
    }

    public static HostImports testModule11() {
        return testModule5();
    }

    public static HostImports testModule13() {
        return testModule3();
    }

    public static HostImports testModule16() {
        return testModule3();
    }

    public static HostImports testModule17() {
        return testModule3();
    }

    public static HostImports testModule18() {
        return testModule5();
    }

    public static HostImports testModule19() {
        return testModule5();
    }

    public static HostImports testModule23() {
        return testModule5();
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

    // TODO: This test shows that we need to push the instance into the FuncRef, this enables simplifying TableInstance too
    public static HostImports testModule30() {
        return new HostImports(
                new HostGlobal[] {new HostGlobal("module4", "f", module4Instance.global(0))});
    }
}
