package com.dylibso.chicory.dwarf.rust;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import org.junit.jupiter.api.Test;

public final class MachinesTest {

    private WasmModule loadModule(String fileName) {
        return Parser.parse(getClass().getResourceAsStream("/" + fileName));
    }

    private String readStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Test
    public void shouldEmitUnderstandableStackTraces() throws Exception {
        var instance =
                Instance.builder(loadModule("compiled/count_vowels.rs.wasm"))
                        .withDebugParser(DebugParser::parse)
                        .build();
        var countVowels = instance.export("count_vowels");
        var exception = assertThrows(TrapException.class, () -> countVowels.apply(0, -1));
        var exceptionTxt = readStackTrace(exception);

        // To generate the following stack track in rust, run:
        //     ./wasm-corpus/run.sh run bash -c "cd rust/count_vowels; RUST_BACKTRACE=1 cargo test"
        //
        //   0: __rustc::rust_begin_unwind
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/std/src/panicking.rs:697:5
        //   1: core::panicking::panic_nounwind_fmt::runtime
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/panicking.rs:117:22
        //   2: core::panicking::panic_nounwind_fmt
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/intrinsics/mod.rs:3241:9
        //   3: core::panicking::panic_nounwind
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/panicking.rs:218:5
        //   4: core::slice::raw::from_raw_parts::precondition_check
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:68:21
        //   5: core::slice::raw::from_raw_parts
        //             at
        // /rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:75:17
        //   6: count_vowels
        //             at ./src/lib.rs:23:26
        //   7: count_vowels::tests::test_count_vowels
        //             at ./src/lib.rs:40:9

        // It's not exactly 1-to-1, but it's close enough.
        assertTrue(
                exceptionTxt.contains(
                        "at 0x00627d: chicory"
                            + " interpreter.begin_panic_handler(library/std/src/panicking.rs:697)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x007e74: chicory"
                            + " interpreter.panic_nounwind_fmt(library/core/src/panicking.rs:117)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x007ec8: chicory"
                            + " interpreter.panic_nounwind(library/core/src/panicking.rs:218)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x001cf6: chicory"
                            + " interpreter.func_30(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:68)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x003948: chicory"
                            + " interpreter.from_raw_parts<u8>(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:75)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x000d7f: chicory interpreter.count_vowels(src/lib.rs:23)"));
    }

    @Test
    public void shouldEmitUnderstandableStackTracesCompiled() throws Exception {
        WasmModule module = loadModule("compiled/count_vowels.rs.wasm");
        var instance =
                Instance.builder(module)
                        .withDebugParser(DebugParser::parse)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module)
                                        .withClassName("com.dylibso.chicory.$gen.CompiledMachine")
                                        .withDebugParser(DebugParser::parse)
                                        .compile())
                        .build();
        var countVowels = instance.export("count_vowels");
        var exception = assertThrows(TrapException.class, () -> countVowels.apply(0, -1));
        var exceptionTxt = readStackTrace(exception);

        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.begin_panic_handler(library/std/src/panicking.rs:697)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.panic_nounwind_fmt(library/core/src/panicking.rs:117)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.panic_nounwind(library/core/src/panicking.rs:218)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.func_30(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:68)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.from_raw_parts<u8>(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:75)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.count_vowels(src/lib.rs:23)"));
    }

    @Test
    public void shouldEmitUnderstandableStackTracesCompiledAndInterpreted() throws Exception {
        WasmModule module = loadModule("compiled/count_vowels.rs.wasm");
        var instance =
                Instance.builder(module)
                        .withDebugParser(DebugParser::parse)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(module)
                                        .withClassName("com.dylibso.chicory.$gen.CompiledMachine")
                                        .withInterpretedFunctions(Set.of((54)))
                                        .withDebugParser(DebugParser::parse)
                                        .compile())
                        .build();
        var countVowels = instance.export("count_vowels");
        var exception = assertThrows(TrapException.class, () -> countVowels.apply(0, -1));
        var exceptionTxt = readStackTrace(exception);

        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.func_30(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:68)"));
        assertTrue(
                exceptionTxt.contains(
                        "at 0x003948: chicory"
                            + " interpreter.from_raw_parts<u8>(/rustc/17067e9ac6d7ecb70e50f92c1944e545188d2359/library/core/src/ub_checks.rs:75)"));
        assertTrue(
                exceptionTxt.contains(
                        "at com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.count_vowels(src/lib.rs:23)"));
    }
}
