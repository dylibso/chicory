package com.dylibso.chicory.source.compiler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Ports of runtime/src/test/java/com/dylibso/chicory/runtime/WasmModuleTest.java
 * using the source compiler as the machine factory.
 */
public class WasmModuleSourceCompilerTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    private static Instance buildInstance(WasmModule module) {
        return buildInstance(module, ImportValues.builder().build());
    }

    private static Instance buildInstance(WasmModule module, ImportValues imports) {
        return Instance.builder(module)
                .withImportValues(imports)
                .withMachineFactory(
                        instance ->
                                MachineFactorySourceCompiler.builder(instance.module())
                                        .withClassName("com.dylibso.chicory.gen.TestMachine")
                                        .withDumpSources(false)
                                        .compile()
                                        .apply(instance))
                .build();
    }

    @Test
    public void shouldWorkFactorial() {
        var instance = buildInstance(loadModule("compiled/iterfact.wat.wasm"));
        var iterFact = instance.export("iterFact");
        var result = iterFact.apply(5L)[0];
        assertEquals(120L, result);
    }

    @Test
    public void shouldRunABasicAdd() {
        var instance = buildInstance(loadModule("compiled/add.wat.wasm"));
        var add = instance.export("add");
        var result = add.apply(5L, 6L)[0];
        assertEquals(11L, result);
    }

    @Test
    public void shouldSupportBrTable() {
        var instance = buildInstance(loadModule("compiled/br_table.wat.wasm"));
        var switchLike = instance.export("switch_like");
        assertEquals(102L, switchLike.apply(0L)[0]);
        assertEquals(101L, switchLike.apply(1)[0]);
        assertEquals(100L, switchLike.apply(2)[0]);
        assertEquals(103L, switchLike.apply(-1)[0]);
        assertEquals(103L, switchLike.apply(3)[0]);
        assertEquals(103L, switchLike.apply(4)[0]);
        assertEquals(103L, switchLike.apply(100)[0]);
    }

    @Test
    public void shouldExerciseBranches() {
        var instance = buildInstance(loadModule("compiled/branching.wat.wasm"));
        var foo = instance.export("foo");
        assertEquals(42L, foo.apply(0)[0]);
        assertEquals(99L, foo.apply(1)[0]);
        for (var i = 2; i < 100; i++) {
            assertEquals(7L, foo.apply(i)[0]);
        }
    }

    @Test
    public void shouldConsoleLogWithString() {
        final AtomicInteger count = new AtomicInteger();
        final String expected = "Hello, World!";

        var func =
                new HostFunction(
                        "console",
                        "log",
                        FunctionType.of(List.of(ValType.I32, ValType.I32), List.of()),
                        (Instance instance, long... args) -> {
                            Memory memory = instance.memory();
                            int len = (int) args[0];
                            int offset = (int) args[1];
                            var message = memory.readString(offset, len);
                            if (expected.equals(message)) {
                                count.incrementAndGet();
                            }
                            return null;
                        });
        var instance =
                buildInstance(
                        loadModule("compiled/host-function.wat.wasm"),
                        ImportValues.builder().addFunction(func).build());
        var logIt = instance.export("logIt");
        logIt.apply();
        assertEquals(10, count.get());
    }

    @Test
    public void shouldComputeFactorial() {
        var instance = buildInstance(loadModule("compiled/iterfact.wat.wasm"));
        var iterFact = instance.export("iterFact");
        for (var i = 0; i < 10; i++) {
            var result = iterFact.apply(i)[0];
            assertEquals(factorial(i), result);
        }
    }

    private static long factorial(int number) {
        long result = 1;
        for (int factor = 2; factor <= number; factor++) {
            result *= factor;
        }
        return result;
    }

    @Test
    public void shouldWorkWithStartFunction() {
        final AtomicInteger count = new AtomicInteger();

        var func =
                new HostFunction(
                        "env",
                        "gotit",
                        FunctionType.of(List.of(ValType.I32), List.of()),
                        (Instance instance, long... args) -> {
                            if (args[0] == 42L) {
                                count.incrementAndGet();
                            }
                            return null;
                        });
        buildInstance(
                loadModule("compiled/start.wat.wasm"),
                ImportValues.builder().addFunction(func).build());
        assertTrue(count.get() > 0);
    }

    @Test
    public void shouldTrapOnUnreachable() {
        var module = loadModule("compiled/trap.wat.wasm");
        var uninstantiable =
                assertThrows(UninstantiableException.class, () -> buildInstance(module));
        assertInstanceOf(TrapException.class, uninstantiable.getCause());
    }

    @Test
    public void shouldSupportGlobals() {
        var instance = buildInstance(loadModule("compiled/globals.wat.wasm"));
        var doit = instance.export("doit");
        var result = doit.apply(32)[0];
        assertEquals(42L, result);
    }

    @Test
    public void shouldCountVowels() {
        var instance = buildInstance(loadModule("compiled/count_vowels.rs.wasm"));
        var alloc = instance.export("alloc");
        var dealloc = instance.export("dealloc");
        var countVowels = instance.export("count_vowels");
        var memory = instance.memory();
        var message = "Hello, World!";
        var len = message.getBytes(UTF_8).length;
        int ptr = (int) alloc.apply(len)[0];
        memory.writeString(ptr, message);
        var result = countVowels.apply(ptr, len);
        dealloc.apply(ptr, len);
        assertEquals(3L, result[0]);
    }

    @Test
    public void shouldRunBasicCProgram() {
        var instance = buildInstance(loadModule("compiled/basic.c.wasm"));
        var run = instance.export("run");
        assertEquals(42L, run.apply()[0]);
    }

    @Test
    public void shouldRunComplexFunction() {
        var instance = buildInstance(loadModule("compiled/complex.c.wasm"));
        var run = instance.export("run");
        assertEquals(-679, (int) run.apply()[0]);
    }

    @Test
    public void shouldRunMemoryProgramInC() {
        var instance = buildInstance(loadModule("compiled/memory.c.wasm"));
        var run = instance.export("run");
        assertEquals(11L, run.apply()[0]);
    }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance = buildInstance(loadModule("compiled/memory.wat.wasm"));
        var run = instance.export("run32");
        assertEquals(42L, run.apply(42)[0]);
        assertEquals(Integer.MAX_VALUE, (int) run.apply(Integer.MAX_VALUE)[0]);
        assertEquals(Integer.MIN_VALUE, (int) run.apply(Integer.MIN_VALUE)[0]);

        run = instance.export("run64");
        assertEquals(42L, run.apply(42L)[0]);
        assertEquals(Long.MIN_VALUE, run.apply(Long.MIN_VALUE)[0]);
        assertEquals(Long.MAX_VALUE, run.apply(Long.MAX_VALUE)[0]);
    }

    @Test
    public void shouldRunKitchenSink() {
        var instance = buildInstance(loadModule("compiled/kitchensink.wat.wasm"));
        var run = instance.export("run");
        assertEquals(6L, run.apply(100)[0]);
    }

    @Test
    public void shouldOperateMemoryOps() {
        var instance = buildInstance(loadModule("compiled/memories.wat.wasm"));
        var run = instance.export("run");
        assertEquals(-25438, (int) run.apply(100)[0]);
    }

    @Test
    public void shouldRunMixedImports() {
        var cbrtFunc =
                new HostFunction(
                        "env",
                        "cbrt",
                        FunctionType.of(List.of(ValType.I32), List.of(ValType.F64)),
                        (Instance instance, long... args) -> {
                            var x = args[0];
                            var cbrt = Math.cbrt((double) x);
                            return new long[] {Double.doubleToRawLongBits(cbrt)};
                        });
        var logResult = new AtomicReference<String>(null);
        var logFunc =
                new HostFunction(
                        "env",
                        "log",
                        FunctionType.of(List.of(ValType.I32, ValType.F64), List.of()),
                        (Instance instance, long... args) -> {
                            var logLevel = args[0];
                            var value = (int) Double.longBitsToDouble(args[1]);
                            logResult.set(logLevel + ": " + value);
                            return null;
                        });
        var memory = new ImportMemory("env", "memory", new ByteBufferMemory(new MemoryLimits(1)));
        var hostImports =
                ImportValues.builder().addFunction(cbrtFunc, logFunc).addMemory(memory).build();
        var instance = buildInstance(loadModule("compiled/mixed-imports.wat.wasm"), hostImports);
        var run = instance.export("main");
        run.apply();
        assertEquals("1: 164", logResult.get());
    }

    @Test
    public void issue294_BRIF() {
        var instance = buildInstance(loadModule("compiled/issue294_brif.wat.wasm"));
        assertEquals(5L, instance.export("main").apply(5)[0]);
    }

    @Test
    public void issue294_BR() {
        var instance = buildInstance(loadModule("compiled/issue294_br.wat.wasm"));
        assertEquals(4L, instance.export("main").apply()[0]);
    }

    @Test
    public void issue294_BRTABLE() {
        var instance = buildInstance(loadModule("compiled/issue294_brtable.wat.wasm"));
        assertEquals(4L, instance.export("main").apply()[0]);
    }

    @Test
    public void shouldEasilyObtainExportedEntities() {
        var instance = buildInstance(loadModule("compiled/exports.wat.wasm"));
        assertNotNull(instance.exports().memory("mem").pages());
        assertNotNull(instance.exports().table("tab").size());
        assertNotNull(instance.exports().global("glob1").getValue());
        assertNotNull(instance.exports().function("get-1").apply());
    }

    @Test
    public void shouldThrowOnInvalidExports() {
        var instance = buildInstance(loadModule("compiled/exports.wat.wasm"));
        assertThrows(InvalidException.class, () -> instance.exports().memory("nonexistent"));
        assertThrows(InvalidException.class, () -> instance.exports().table("nonexistent"));
        assertThrows(InvalidException.class, () -> instance.exports().global("nonexistent"));
        assertThrows(InvalidException.class, () -> instance.exports().function("nonexistent"));
        assertThrows(InvalidException.class, () -> instance.exports().memory("tab"));
        assertThrows(InvalidException.class, () -> instance.exports().table("mem"));
        assertThrows(InvalidException.class, () -> instance.exports().global("get-1"));
        assertThrows(InvalidException.class, () -> instance.exports().function("glob1"));
    }

    @Test
    public void shouldImportAliases() {
        AtomicBoolean logged1 = new AtomicBoolean(false);
        AtomicBoolean logged2 = new AtomicBoolean(false);
        var logFn =
                new HostFunction(
                        "env",
                        "log",
                        FunctionType.of(List.of(ValType.I32), List.of()),
                        (inst, args) -> {
                            logged1.set(true);
                            return null;
                        });
        var logWrongSignatureFn =
                new HostFunction(
                        "env",
                        "log",
                        FunctionType.of(List.of(ValType.I64), List.of()),
                        (inst, args) -> {
                            logged2.set(true);
                            return null;
                        });
        var imports =
                ImportValues.builder().addFunction(logFn).addFunction(logWrongSignatureFn).build();
        var instance = buildInstance(loadModule("compiled/alias-imports1.wat.wasm"), imports);
        instance.exports().function("log").apply();
        instance.exports().function("log-alias").apply();
        assertTrue(logged1.get());
        assertFalse(logged2.get());
        assertEquals(2, instance.imports().functionCount());
    }

    @Test
    public void shouldResolveMultipleAliasesByType() {
        AtomicBoolean loggedI32 = new AtomicBoolean(false);
        AtomicBoolean loggedI64 = new AtomicBoolean(false);
        var logI32 =
                new HostFunction(
                        "env",
                        "log",
                        FunctionType.of(List.of(ValType.I32), List.of()),
                        (inst, args) -> {
                            loggedI32.set(true);
                            return null;
                        });
        var logI64 =
                new HostFunction(
                        "env",
                        "log",
                        FunctionType.of(List.of(ValType.I64), List.of()),
                        (inst, args) -> {
                            loggedI64.set(true);
                            return null;
                        });
        var imports = ImportValues.builder().addFunction(logI32).addFunction(logI64).build();
        var instance = buildInstance(loadModule("compiled/alias-imports2.wat.wasm"), imports);
        instance.exports().function("log-i32").apply(0);
        assertTrue(loggedI32.get());
        assertFalse(loggedI64.get());
        instance.exports().function("log-i64").apply(0);
        assertTrue(loggedI32.get());
        assertTrue(loggedI64.get());
    }

    @Test
    public void testExternrefHandling() {
        var testObject = new Object();
        var sideTable = new java.util.HashMap<Long, Object>();
        var imports =
                ImportValues.builder()
                        .addFunction(
                                new HostFunction(
                                        "env",
                                        "get_host_object",
                                        FunctionType.of(List.of(), List.of(ValType.ExternRef)),
                                        (inst, args) -> {
                                            sideTable.put(123L, testObject);
                                            return new long[] {123L};
                                        }))
                        .addFunction(
                                new HostFunction(
                                        "env",
                                        "is_null",
                                        FunctionType.of(
                                                List.of(ValType.ExternRef), List.of(ValType.I32)),
                                        (inst, args) -> {
                                            long key = args[0];
                                            return (sideTable.get(key) == null)
                                                    ? new long[] {1}
                                                    : new long[] {0};
                                        }))
                        .build();
        var instance = buildInstance(loadModule("compiled/externref-example.wat.wasm"), imports);
        assertEquals(123L, instance.exports().function("process_externref").apply(123L)[0]);
        assertEquals(1L, instance.exports().function("is_null").apply(123L)[0]);
        var ref = instance.exports().function("get_host_object").apply()[0];
        assertEquals(123L, ref);
        assertEquals(0L, instance.exports().function("is_null").apply(123L)[0]);
        assertEquals(1L, instance.exports().function("is_null").apply(1L)[0]);
    }
}
