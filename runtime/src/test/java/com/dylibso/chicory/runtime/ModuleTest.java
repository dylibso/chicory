package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.exceptions.UninstantiableException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ModuleTest {

    private static Module loadModule(String fileName) {
        return Module.builder(ModuleTest.class.getResourceAsStream("/" + fileName)).build();
    }

    @Test
    public void shouldWorkFactorial() {
        var instance = Instance.builder(loadModule("compiled/iterfact.wat.wasm")).build();
        var iterFact = instance.export("iterFact");
        var result = iterFact.apply(Value.i32(5))[0];
        assertEquals(120, result.asInt());
    }

    @Test
    public void shouldSupportBrTable() {
        var instance = Instance.builder(loadModule("compiled/br_table.wat.wasm")).build();
        var switchLike = instance.export("switch_like");
        var result = switchLike.apply(Value.i32(0))[0];
        assertEquals(102, result.asInt());
        result = switchLike.apply(Value.i32(1))[0];
        assertEquals(101, result.asInt());
        result = switchLike.apply(Value.i32(2))[0];
        assertEquals(100, result.asInt());
        result = switchLike.apply(Value.i32(-1))[0];
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(3))[0];
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(4))[0];
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(100))[0];
        assertEquals(103, result.asInt());
    }

    @Test
    public void shouldExerciseBranches() {
        var module = Instance.builder(loadModule("compiled/branching.wat.wasm")).build();
        var foo = module.export("foo");

        var result = foo.apply(Value.i32(0))[0];
        assertEquals(42, result.asInt());

        result = foo.apply(Value.i32(1))[0];
        assertEquals(99, result.asInt());

        for (var i = 2; i < 100; i++) {
            result = foo.apply(Value.i32(i))[0];
            assertEquals(7, result.asInt());
        }
    }

    @Test
    public void shouldConsoleLogWithString() {
        final AtomicInteger count = new AtomicInteger();
        final String expected = "Hello, World!";

        var func =
                new HostFunction(
                        (Instance instance,
                                Value... args) -> { // decompiled is: console_log(13, 0);
                            Memory memory = instance.memory();
                            var len = args[0].asInt();
                            var offset = args[1].asInt();
                            var message = memory.readString(offset, len);

                            if (expected.equals(message)) {
                                count.incrementAndGet();
                            }

                            return null;
                        },
                        "console",
                        "log",
                        List.of(ValueType.I32, ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        var instance =
                Instance.builder(loadModule("compiled/host-function.wat.wasm"))
                        .withHostImports(new HostImports(funcs))
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
            var result = iterFact.apply(Value.i32(i))[0];
            // test against an oracle Java implementation
            assertEquals(factorial(i), result.asInt());
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
                        (Instance instance, Value... args) -> {
                            var val = args[0];

                            if (val.asInt() == 42) {
                                count.incrementAndGet();
                            }

                            return null;
                        },
                        "env",
                        "gotit",
                        List.of(ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        Instance.builder(loadModule("compiled/start.wat.wasm"))
                .withHostImports(new HostImports(funcs))
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
        var result = doit.apply(Value.i32(32))[0];
        assertEquals(42, result.asInt());
    }

    @Test
    public void shouldCountVowels() {
        var instance = Instance.builder(loadModule("compiled/count_vowels.rs.wasm")).build();
        var alloc = instance.export("alloc");
        var dealloc = instance.export("dealloc");
        var countVowels = instance.export("count_vowels");
        var memory = instance.memory();
        var message = "Hello, World!";
        var len = message.getBytes().length;
        var ptr = alloc.apply(Value.i32(len))[0].asInt();
        memory.writeString(ptr, message);
        var result = countVowels.apply(Value.i32(ptr), Value.i32(len));
        dealloc.apply(Value.i32(ptr), Value.i32(len));
        assertEquals(3, result[0].asInt());
    }

    @Test
    public void shouldRunBasicCProgram() {
        // check with: wasmtime src/test/resources/wasm/basic.c.wasm --invoke run
        var instance = Instance.builder(loadModule("compiled/basic.c.wasm")).build();
        var run = instance.export("run");
        var result = run.apply()[0];
        assertEquals(42, result.asInt());
    }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance = Instance.builder(loadModule("compiled/memory.wat.wasm")).build();
        var run = instance.export("run32");
        var results = run.apply(Value.i32(42));
        var result = results[0];
        assertEquals(42, result.asInt());

        result = run.apply(Value.i32(Integer.MAX_VALUE))[0];
        assertEquals(Integer.MAX_VALUE, result.asInt());

        result = run.apply(Value.i32(Integer.MIN_VALUE))[0];
        assertEquals(Integer.MIN_VALUE, result.asInt());

        run = instance.export("run64");
        result = run.apply(Value.i64(42))[0];
        assertEquals(42L, result.asLong());

        run = instance.export("run64");
        result = run.apply(Value.i64(Long.MIN_VALUE))[0];
        assertEquals(Long.MIN_VALUE, result.asLong());

        run = instance.export("run64");
        result = run.apply(Value.i64(Long.MAX_VALUE))[0];
        assertEquals(Long.MAX_VALUE, result.asLong());
    }

    @Test
    public void shouldRunKitchenSink() {
        // check with: wasmtime src/test/resources/wasm/kitchensink.wat.wasm --invoke
        // run 100
        var instance = Instance.builder(loadModule("compiled/kitchensink.wat.wasm")).build();

        var run = instance.export("run");
        assertEquals(6, run.apply(Value.i32(100))[0].asInt());
    }

    // @Test
    // public void shouldOperateMemoryOps() {
    // // check with: wasmtime src/test/resources/wasm/memories.wat.wasm --invoke
    // run 100
    // var instance =
    // Module.builder("asm/memories.wat.wasm").instantiate();
    // var run = instance.getExport("run");
    // assertEquals(-25438, run.apply(Value.i32(100)).asInt());
    // }

    @Test
    public void shouldRunMixedImports() {
        var cbrtFunc =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            var x = args[0].asInt();
                            var cbrt = Math.cbrt(x);
                            return new Value[] {Value.fromDouble(cbrt)};
                        },
                        "env",
                        "cbrt",
                        List.of(ValueType.I32),
                        List.of(ValueType.F64));
        var logResult = new AtomicReference<String>(null);
        var logFunc =
                new HostFunction(
                        (Instance instance, Value... args) -> {
                            var logLevel = args[0].asInt();
                            var value = (int) args[1].asDouble();
                            logResult.set(logLevel + ": " + value);
                            return null;
                        },
                        "env",
                        "log",
                        List.of(ValueType.I32, ValueType.F64),
                        List.of());
        var memory = new HostMemory("env", "memory", new Memory(new MemoryLimits(1)));

        var hostImports =
                new HostImports(
                        new HostFunction[] {cbrtFunc, logFunc},
                        new HostGlobal[0],
                        memory,
                        new HostTable[0]);
        var instance =
                Instance.builder(loadModule("compiled/mixed-imports.wat.wasm"))
                        .withHostImports(hostImports)
                        .build();

        var run = instance.export("main");
        run.apply();
        assertEquals("1: 164", logResult.get());
    }

    @Test
    public void issue294_BRIF() {
        var instance = Instance.builder(loadModule("compiled/issue294_brif.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(5, main.apply(Value.i32(5))[0].asInt());
    }

    @Test
    public void issue294_BR() {
        var instance = Instance.builder(loadModule("compiled/issue294_br.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(4, main.apply()[0].asInt());
    }

    @Test
    public void issue294_BRTABLE() {
        var instance = Instance.builder(loadModule("compiled/issue294_brtable.wat.wasm")).build();

        var main = instance.export("main");
        assertEquals(4, main.apply()[0].asInt());
    }

    @Test
    public void shouldCountNumberOfInstructions() {
        AtomicLong count = new AtomicLong(0);
        var instance =
                Instance.builder(loadModule("compiled/iterfact.wat.wasm"))
                        .withUnsafeExecutionListener(
                                (Instruction instruction, long[] operands, MStack stack) ->
                                        count.getAndIncrement())
                        .build();
        var iterFact = instance.export("iterFact");

        iterFact.apply(Value.i32(100));

        // current result is: 1109
        assertTrue(count.get() > 0);
        assertTrue(count.get() < 2000);
    }

    @Test
    public void shouldValidateTypes() {
        assertDoesNotThrow(
                () ->
                        Instance.builder(loadModule("compiled/i32.wat.wasm"))
                                .withTypeValidation(true)
                                .build());
    }

    @Test
    public void shouldConsumeStackLoopOperations() {
        AtomicLong finalStackSize = new AtomicLong(0);
        var instance =
                Instance.builder(loadModule("compiled/fac.wat.wasm"))
                        .withUnsafeExecutionListener(
                                (Instruction instruction, long[] operands, MStack stack) -> {
                                    finalStackSize.set(stack.size());
                                })
                        .build();
        var facSsa = instance.export("fac-ssa");

        var number = 100;
        var result = facSsa.apply(Value.i32(number));
        assertEquals(factorial(number), result[0].asInt());

        // IIUC: 3 values returning from last CALL + 1 result
        assertTrue(finalStackSize.get() == 4L);
    }
}
