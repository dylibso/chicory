package com.dylibso.chicory.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.UninstantiableException;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ModuleTest {

    private static Module loadModule(String fileName) {
        return Parser.parse(ModuleTest.class.getResourceAsStream("/" + fileName));
    }

    @Test
    public void shouldWorkFactorial() {
        var instance = Instance.builder(loadModule("compiled/iterfact.wat.wasm")).build();
        var iterFact = instance.export("iterFact");
        var result = iterFact.apply(5L)[0];
        assertEquals(120L, result);
    }

    @Test
    public void shouldRunABasicAdd() {
        var instance = Instance.builder(loadModule("compiled/add.wat.wasm")).build();
        var add = instance.export("add");
        var result = add.apply(5L, 6L)[0];
        assertEquals(11L, result);
    }

    @Test
    public void shouldSupportBrTable() {
        var instance = Instance.builder(loadModule("compiled/br_table.wat.wasm")).build();
        var switchLike = instance.export("switch_like");
        var result = switchLike.apply(0L)[0];
        assertEquals(102L, result);
        result = switchLike.apply(1)[0];
        assertEquals(101L, result);
        result = switchLike.apply(2)[0];
        assertEquals(100L, result);
        result = switchLike.apply(-1)[0];
        assertEquals(103L, result);
        result = switchLike.apply(3)[0];
        assertEquals(103L, result);
        result = switchLike.apply(4)[0];
        assertEquals(103L, result);
        result = switchLike.apply(100)[0];
        assertEquals(103L, result);
    }

    @Test
    public void shouldExerciseBranches() {
        var module = Instance.builder(loadModule("compiled/branching.wat.wasm")).build();
        var foo = module.export("foo");

        var result = foo.apply(0)[0];
        assertEquals(42L, result);

        result = foo.apply(1)[0];
        assertEquals(99L, result);

        for (var i = 2; i < 100; i++) {
            result = foo.apply(i)[0];
            assertEquals(7L, result);
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
                        (Instance instance, long... args) -> { // decompiled is: console_log(13, 0);
                            Memory memory = instance.memory();
                            int len = (int) args[0];
                            int offset = (int) args[1];
                            var message = memory.readString(offset, len);

                            if (expected.equals(message)) {
                                count.incrementAndGet();
                            }

                            return null;
                        },
                        List.of(ValueType.I32, ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        var instance =
                Instance.builder(loadModule("compiled/host-function.wat.wasm"))
                        .withExternalValues(new ExternalValues(funcs))
                        .build();
        var logIt = instance.export("logIt");
        logIt.apply();

        assertEquals(10, count.get());
    }

    @Test
    public void shouldComputeFactorial() {
        var instance = Instance.builder(loadModule("compiled/iterfact.wat.wasm")).build();
        var iterFact = instance.export("iterFact");

        // don't make this too big we will overflow 32 bits
        for (var i = 0; i < 10; i++) {
            var result = iterFact.apply(i)[0];
            // test against an oracle Java implementation
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
                        (Instance instance, long... args) -> {
                            var val = args[0];

                            if (val == 42L) {
                                count.incrementAndGet();
                            }

                            return null;
                        },
                        List.of(ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        Instance.builder(loadModule("compiled/start.wat.wasm"))
                .withExternalValues(new ExternalValues(funcs))
                .build();

        assertTrue(count.get() > 0);
    }

    @Test
    public void shouldTrapOnUnreachable() {
        var instanceBuilder = Instance.builder(loadModule("compiled/trap.wat.wasm"));
        assertThrows(UninstantiableException.class, () -> instanceBuilder.build());
    }

    @Test
    public void shouldSupportGlobals() {
        var instance = Instance.builder(loadModule("compiled/globals.wat.wasm")).build();
        var doit = instance.export("doit");
        var result = doit.apply(32)[0];
        assertEquals(42L, result);
    }

    @Test
    public void shouldCountVowels() {
        var instance = Instance.builder(loadModule("compiled/count_vowels.rs.wasm")).build();
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
        // check with: wasmtime basic.c.wasm --invoke run
        var instance = Instance.builder(loadModule("compiled/basic.c.wasm")).build();
        var run = instance.export("run");
        var result = run.apply()[0];
        assertEquals(42L, result);
    }

    @Test
    public void shouldRunComplexFunction() {
        // check with: wasmtime complex.c.wasm --invoke run
        var instance = Instance.builder(loadModule("compiled/complex.c.wasm")).build();
        var run = instance.export("run");
        var result = run.apply();
        assertEquals(-679, (int) result[0]);
    }

    @Test
    public void shouldRunMemoryProgramInC() {
        // check with: wasmtime memory.c.wasm --invoke run
        var instance = Instance.builder(loadModule("compiled/memory.c.wasm")).build();
        var run = instance.export("run");
        var result = run.apply();
        assertEquals(11L, result[0]);
    }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance = Instance.builder(loadModule("compiled/memory.wat.wasm")).build();
        var run = instance.export("run32");
        var results = run.apply(42);
        var result = results[0];
        assertEquals(42L, result);

        result = run.apply(Integer.MAX_VALUE)[0];
        assertEquals(Integer.MAX_VALUE, (int) result);

        result = run.apply(Integer.MIN_VALUE)[0];
        assertEquals(Integer.MIN_VALUE, (int) result);

        run = instance.export("run64");
        result = run.apply(42L)[0];
        assertEquals(42L, result);

        run = instance.export("run64");
        result = run.apply(Long.MIN_VALUE)[0];
        assertEquals(Long.MIN_VALUE, result);

        run = instance.export("run64");
        result = run.apply(Long.MAX_VALUE)[0];
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void shouldRunKitchenSink() {
        // check with: wasmtime kitchensink.wat.wasm --invoke
        // run 100
        var instance = Instance.builder(loadModule("compiled/kitchensink.wat.wasm")).build();

        var run = instance.export("run");
        assertEquals(6L, run.apply(100)[0]);
    }

    @Test
    public void shouldOperateMemoryOps() {
        // check with: wasmtime memories.wat.wasm --invoke run 100
        var instance = Instance.builder(loadModule("compiled/memories.wat.wasm")).build();
        var run = instance.export("run");
        assertEquals(-25438, (int) run.apply(100)[0]);
    }

    @Test
    public void shouldRunMixedImports() {
        var cbrtFunc =
                new HostFunction(
                        "env",
                        "cbrt",
                        (Instance instance, long... args) -> {
                            var x = args[0];
                            var cbrt = Math.cbrt((double) x);
                            return new long[] {Double.doubleToRawLongBits(cbrt)};
                        },
                        List.of(ValueType.I32),
                        List.of(ValueType.F64));
        var logResult = new AtomicReference<String>(null);
        var logFunc =
                new HostFunction(
                        "env",
                        "log",
                        (Instance instance, long... args) -> {
                            var logLevel = args[0];
                            var value = (int) Double.longBitsToDouble(args[1]);
                            logResult.set(logLevel + ": " + value);
                            return null;
                        },
                        List.of(ValueType.I32, ValueType.F64),
                        List.of());
        var memory = new ExternalMemory("env", "memory", new Memory(new MemoryLimits(1)));

        var hostImports =
                new ExternalValues(
                        new HostFunction[] {cbrtFunc, logFunc},
                        new ExternalGlobal[0],
                        memory,
                        new ExternalTable[0]);
        var instance =
                Instance.builder(loadModule("compiled/mixed-imports.wat.wasm"))
                        .withExternalValues(hostImports)
                        .build();

        var run = instance.export("main");
        run.apply();
        assertEquals("1: 164", logResult.get());
    }

    @Test
    public void issue294_BRIF() {
        var instance = Instance.builder(loadModule("compiled/issue294_brif.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(5L, main.apply(5)[0]);
    }

    @Test
    public void issue294_BR() {
        var instance = Instance.builder(loadModule("compiled/issue294_br.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(4L, main.apply()[0]);
    }

    @Test
    public void issue294_BRTABLE() {
        var instance = Instance.builder(loadModule("compiled/issue294_brtable.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(4L, main.apply()[0]);
    }

    @Test
    public void shouldCountNumberOfInstructions() {
        AtomicLong count = new AtomicLong(0);
        var instance =
                Instance.builder(loadModule("compiled/iterfact.wat.wasm"))
                        .withUnsafeExecutionListener(
                                (instruction, stack) -> count.getAndIncrement())
                        .build();
        var iterFact = instance.export("iterFact");

        iterFact.apply(100L);

        // current result is: 1109
        assertTrue(count.get() > 0);
        assertTrue(count.get() < 2000);
    }

    @Test
    public void shouldConsumeStackLoopOperations() {
        AtomicLong finalStackSize = new AtomicLong(0);
        var instance =
                Instance.builder(loadModule("compiled/fac.wat.wasm"))
                        .withUnsafeExecutionListener(
                                (instruction, stack) -> finalStackSize.set(stack.size()))
                        .build();
        var facSsa = instance.export("fac-ssa");

        var number = 100;
        var result = facSsa.apply(number);
        assertEquals(factorial(number), result[0]);

        // IIUC: 3 values returning from last CALL + 1 result
        assertTrue(finalStackSize.get() == 4L);
    }

    @Test
    public void shouldIgnoreMissingImports() {
        Instance.builder(loadModule("compiled/hello-wasi.wat.wasm"))
                .withStart(false)
                .withInitialize(false)
                .withSkipImportMapping(true)
                .build();
    }
}
