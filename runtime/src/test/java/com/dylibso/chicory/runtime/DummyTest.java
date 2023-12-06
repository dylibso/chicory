package com.dylibso.chicory.runtime;

import com.dylibso.chicory.imports.SpecV1ImportsHostFuncs;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import org.junit.jupiter.api.Test;

// JUST TESTING: will be removed before finalizing the PR
public class DummyTest {

    Instance testModule1 =
            Module.build(new File("target/compiled-wast/imports/spec.1.wasm"))
                    .instantiate(SpecV1ImportsHostFuncs.fallback());

    //    TestModule testModule38 = TestModule.of(new
    // File("target/compiled-wast/imports/spec.95.wasm")).build().instantiate(SpecV1ImportsHostFuncs.fallback());

    @Test
    public void test1() {
        ExportFunction varPrint64 = testModule1.getExport("print64");
        var results = varPrint64.apply(Value.i64(Long.parseUnsignedLong("24")));
    }
    //    @Test()
    //    public void test0() {
    //                ExportFunction varPrint32 = testModule1.getExport("print32");
    //                var results = varPrint32.apply(Value.i32(Integer.parseUnsignedInt("13")));
    //    }

    //
    //    Instance testModule2 = Module.build(new
    // File("target/compiled-wast/imports/spec.3.wasm")).instantiate(SpecV1ImportsHostFuncs.funcs1());
    //
    //    @Test()
    //    public void test3() {
    //        ExportFunction varPrintI32 = testModule2.getExport("print_i32");
    //        var results = varPrintI32.apply(Value.i32(Integer.parseUnsignedInt("13")));
    //    }
    //
    //    Instance testModule3 = Module.build(new
    // File("target/compiled-wast/imports/spec.4.wasm")).instantiate(SpecV1ImportsHostFuncs.funcs1());
    //
    //    @Test
    //    public void test4() {
    //        ExportFunction varPrintI32 = testModule3.getExport("print_i32");
    //        var results = varPrintI32.apply(Value.i32(Integer.parseUnsignedInt("5")),
    // Value.i32(Integer.parseUnsignedInt("11")));
    //        assertEquals(Integer.parseUnsignedInt("16"), results[0].asInt());
    //    }

    //    Instance testModule4 =
    //            Module.build(new File("target/compiled-wast/imports/spec.5.wasm"))
    //                    .instantiate(SpecV1ImportsHostFuncs.fallback());
}
