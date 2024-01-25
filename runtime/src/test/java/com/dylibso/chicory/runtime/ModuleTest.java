package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ModuleTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldWorkFactorial() {
        var module = Module.builder("compiled/iterfact.wat.wasm").build();
        var instance = module.instantiate();
        var iterFact = instance.export("iterFact");
        var result = iterFact.apply(Value.i32(5))[0];
        assertEquals(120, result.asInt());
    }

    @Test
    public void shouldSupportBrTable() {
        var instance = Module.builder("compiled/br_table.wat.wasm").build().instantiate();
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
        var module = Module.builder("compiled/branching.wat.wasm").build().instantiate();
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
                Module.builder("compiled/host-function.wat.wasm")
                        .build()
                        .instantiate(new HostImports(funcs));
        var logIt = instance.export("logIt");
        logIt.apply();

        assertEquals(10, count.get());
    }

    @Test
    public void shouldComputeFactorial() {
        var module = Module.builder("compiled/iterfact.wat.wasm").build().instantiate();
        var iterFact = module.export("iterFact");

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
        var module = Module.builder("compiled/start.wat.wasm").build();
        module.instantiate(new HostImports(funcs));

        assertTrue(count.get() > 0);
    }

    @Test
    public void shouldTrapOnUnreachable() {
        var module = Module.builder("compiled/trap.wat.wasm").build();
        assertThrows(WASMMachineException.class, module::instantiate);
    }

    @Test
    public void shouldSupportGlobals() {
        var instance = Module.builder("compiled/globals.wat.wasm").build().instantiate();
        var doit = instance.export("doit");
        var result = doit.apply(Value.i32(32))[0];
        assertEquals(42, result.asInt());
    }

    @Test
    public void shouldCountVowels() {
        var instance = Module.builder("compiled/count_vowels.rs.wasm").build().instantiate();
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
        var instance = Module.builder("compiled/basic.c.wasm").build().instantiate();
        var run = instance.export("run");
        var result = run.apply()[0];
        assertEquals(42, result.asInt());
    }

    // @Test
    // public void shouldRunComplexFunction() {
    // // check with: wasmtime src/test/resources/wasm/complex.c.wasm --invoke run
    // var instance =
    // Module.builder("wasm/complex.c.wasm").instantiate();
    // var run = instance.getExport("run");
    // var result = run.apply();
    // assertEquals(-679, result.asInt());
    // }

    // @Test
    // public void shouldRunMemoryProgramInC() {
    // // check with: wasmtime src/test/resources/wasm/memory.c.wasm --invoke run
    // var instance =
    // Module.builder(("wasm/memory.c.wasm").instantiate();
    // var run = instance.getExport("run");
    // var result = run.apply();
    // assertEquals(11, result.asInt());
    // }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance = Module.builder("compiled/memory.wat.wasm").build().instantiate();
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
        var instance = Module.builder("compiled/kitchensink.wat.wasm").build().instantiate();

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
}
