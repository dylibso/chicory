package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

class Printer {

    private int count = 0;
    private final String expected;

    Printer() {
        this.expected = null;
    }
    Printer(String expected) {
        this.expected = expected;
    }
    public void println(String msg) {
        if (expected == null || expected.equals(msg)) {
            count++;
        }
        System.out.println(msg);
    }

    public int times() {
        return count;
    }
}

public class ModuleTest
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldWorkFactorial()
    {
        var module = Module.build("src/test/resources/wasm/iterfact.wat.wasm");
        var instance = module.instantiate();
        var iterFact = instance.getExport("iterFact");
        var result = iterFact.apply(Value.i32(5));
        assertEquals(120, result.asInt());
    }

    @Test
    public void shouldSupportBrTable() {
        var instance = Module.build("src/test/resources/wasm/br_table.wat.wasm").instantiate();
        var switchLike = instance.getExport("switch_like");
        var result = switchLike.apply(Value.i32(0));
        assertEquals(102, result.asInt());
        result = switchLike.apply(Value.i32(1));
        assertEquals(101, result.asInt());
        result = switchLike.apply(Value.i32(2));
        assertEquals(100, result.asInt());
        result = switchLike.apply(Value.i32(-1));
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(3));
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(4));
        assertEquals(103, result.asInt());
        result = switchLike.apply(Value.i32(100));
        assertEquals(103, result.asInt());
    }

    @Test
    public void shouldExerciseBranches() {
        var module = Module.build("src/test/resources/wasm/branching.wat.wasm").instantiate();
        var foo = module.getExport("foo");

        var result = foo.apply(Value.i32(0));
        assertEquals(42, result.asInt());

        result = foo.apply(Value.i32(1));
        assertEquals(99, result.asInt());

        for (var i = 2; i < 100; i++) {
            result = foo.apply(Value.i32(i));
            assertEquals(7, result.asInt());
        }
    }

    @Test
    public void shouldConsoleLogWithString() {
        var printer = new Printer("Hello, World!");
        var func = new HostFunction(
                (Memory memory, Value... args) -> {
                    var offset = args[0].asInt();
                    var len = args[1].asInt();
                    var message = memory.getString(offset, len);
                    printer.println(message);
                    return null;
                },
                "console",
                "log",
                List.of(ValueType.I32, ValueType.I32),
                List.of()
        );
        var funcs = new HostFunction[]{func};
        var instance = Module.build("src/test/resources/wasm/host-function.wat.wasm").instantiate(funcs);
        var logIt = instance.getExport("logIt");
        logIt.apply();
        assertEquals(10, printer.times());
    }


    @Test
    public void shouldComputeFactorial() {
        var module = Module.build("src/test/resources/wasm/iterfact.wat.wasm").instantiate();
        var iterFact = module.getExport("iterFact");

        // don't make this too big we will overflow 32 bits
        for (var i = 0; i < 10; i++) {
            var result = iterFact.apply(Value.i32(i));
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
        var printer = new Printer("gotit 42");
        var func = new HostFunction(
                (Memory memory, Value... args) -> {
                    var val = args[0];
                    printer.println("gotit " + val.asInt());
                    return null;
                },
                "env",
                "gotit",
                List.of(ValueType.I32),
                List.of()
        );
        var funcs = new HostFunction[]{func};
        var module = Module.build("src/test/resources/wasm/start.wat.wasm").instantiate(funcs);
        var start = module.getExport("_start");
        start.apply();
        assertTrue(printer.times() > 0);
    }


    @Test
    public void shouldTrapOnUnreachable() {
        var instance = Module.build("src/test/resources/wasm/trap.wat.wasm").instantiate();
        var start = instance.getExport("_start");
        assertThrows(TrapException.class, start::apply);
    }

    @Test
    public void shouldSupportGlobals() {
        var instance = Module.build("src/test/resources/wasm/globals.wat.wasm").instantiate();
        var doit = instance.getExport("doit");
        var result = doit.apply(Value.i32(32));
        assertEquals(42, result.asInt());
    }

//    @Test
//    public void shouldCountVowels() {
//        var instance = Module.build("src/test/resources/wasm/count_vowels.rs.wasm").instantiate();
//        var countVowels = instance.getExport("count_vowels");
//        //var memory = module.getMemory();
//        //memory.put(60000, "Hello World!");
//        //var result = countVowels.apply(Value.i32(60000));
//        var result = countVowels.apply();
//        assertEquals(3, result.asInt());
//    }

//    @Test
//    public void shouldRunBasicCProgram() {
//        // check with: wasmtime src/test/resources/wasm/basic.c.wasm --invoke run
//        var instance = Module.build("src/test/resources/wasm/basic.c.wasm").instantiate();
//        var run = instance.getExport("run");
//        var result = run.apply();
//        assertEquals(42, result.asInt());
//    }

//    @Test
//    public void shouldRunComplexFunction() {
//        // check with: wasmtime src/test/resources/wasm/complex.c.wasm --invoke run
//        var instance = Module.build("src/test/resources/wasm/complex.c.wasm").instantiate();
//        var run = instance.getExport("run");
//        var result = run.apply();
//        assertEquals(-679, result.asInt());
//    }

//    @Test
//    public void shouldRunMemoryProgramInC() {
//        // check with: wasmtime src/test/resources/wasm/memory.c.wasm --invoke run
//        var instance = Module.build("src/test/resources/wasm/memory.c.wasm").instantiate();
//        var run = instance.getExport("run");
//        var result = run.apply();
//        assertEquals(11, result.asInt());
//    }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance = Module.build("src/test/resources/wasm/memory.wat.wasm").instantiate();
        var run = instance.getExport("run32");
        var result = run.apply(Value.i32(42));
        assertEquals(42, result.asInt());

        result = run.apply(Value.i32(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, result.asInt());

        result = run.apply(Value.i32(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, result.asInt());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(42));
        assertEquals(42L, result.asLong());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, result.asLong());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, result.asLong());
    }

    @Test
    public void shouldRunKitchenSink() {
        // check with: wasmtime src/test/resources/wasm/kitchensink.wat.wasm --invoke run 100
        var instance = Module.build("src/test/resources/wasm/kitchensink.wat.wasm").instantiate();
        var run = instance.getExport("run");
        assertEquals(6, run.apply(Value.i32(100)).asInt());
    }

//    @Test
//    public void shouldOperateMemoryOps() {
//        // check with: wasmtime src/test/resources/wasm/memories.wat.wasm --invoke run 100
//        var instance = Module.build("src/test/resources/wasm/memories.wat.wasm").instantiate();
//        var run = instance.getExport("run");
//        assertEquals(-25438, run.apply(Value.i32(100)).asInt());
//    }
}
