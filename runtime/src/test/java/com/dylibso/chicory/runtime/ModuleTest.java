package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.dylibso.chicory.runtime.wasi.WasiOptions;
import com.dylibso.chicory.runtime.wasi.WasiP1;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockPrintStream extends PrintStream {
    private ByteArrayOutputStream baos;
    private int times;

    public MockPrintStream() {
        super(new ByteArrayOutputStream());
        this.baos = (ByteArrayOutputStream) this.out;
        this.times = 0;
    }

    @Override
    public void println(String s) {
        super.println(s);
        this.times++;
    }

    public String getOutput() {
        return baos.toString();
    }

    public int getTimes() {
        return times;
    }
}

public class ModuleTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldWorkFactorial() {
        var module = Module.build(new File("src/test/resources/compiled/iterfact.wat.wasm"));
        var instance = module.instantiate();
        var iterFact = instance.getExport("iterFact");
        var result = iterFact.apply(Value.i32(5))[0];
        assertEquals(120, result.asInt());
    }

    @Test
    public void shouldSupportBrTable() {
        var instance =
                Module.build(new File("src/test/resources/compiled/br_table.wat.wasm"))
                        .instantiate();
        var switchLike = instance.getExport("switch_like");
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
        var module =
                Module.build(new File("src/test/resources/compiled/branching.wat.wasm"))
                        .instantiate();
        var foo = module.getExport("foo");

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
        var expected = "Hello, World!";
        var printer = new MockPrintStream();
        var func =
                new HostFunction(
                        (Memory memory, Value... args) -> { // decompiled is: console_log(13, 0);
                            var len = args[0].asInt();
                            var offset = args[1].asInt();
                            var message = memory.readString(offset, len);
                            printer.println(message);
                            return null;
                        },
                        "console",
                        "log",
                        List.of(ValueType.I32, ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        var instance =
                Module.build(new File("src/test/resources/compiled/host-function.wat.wasm"))
                        .instantiate(new HostImports(funcs));
        var logIt = instance.getExport("logIt");
        logIt.apply();
        assertEquals((expected + "\n").repeat(10), printer.getOutput());
        assertEquals(10, printer.getTimes());
    }

    @Test
    public void shouldComputeFactorial() {
        var module =
                Module.build(new File("src/test/resources/compiled/iterfact.wat.wasm"))
                        .instantiate();
        var iterFact = module.getExport("iterFact");

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
        var expected = "gotit 42";
        var printer = new MockPrintStream();
        var func =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            var val = args[0];
                            printer.println("gotit " + val.asInt());
                            return null;
                        },
                        "env",
                        "gotit",
                        List.of(ValueType.I32),
                        List.of());
        var funcs = new HostFunction[] {func};
        var module =
                Module.build(new File("src/test/resources/compiled/start.wat.wasm"))
                        .instantiate(new HostImports(funcs));
        var start = module.getExport("_start");
        start.apply();
        assertEquals(expected + "\n", printer.getOutput());
        assertTrue(printer.getTimes() > 0);
    }

    @Test
    public void shouldTrapOnUnreachable() {
        var instance =
                Module.build(new File("src/test/resources/compiled/trap.wat.wasm")).instantiate();
        var start = instance.getExport("_start");
        assertThrows(TrapException.class, start::apply);
    }

    @Test
    public void shouldSupportGlobals() {
        var instance =
                Module.build(new File("src/test/resources/compiled/globals.wat.wasm"))
                        .instantiate();
        var doit = instance.getExport("doit");
        var result = doit.apply(Value.i32(32))[0];
        assertEquals(42, result.asInt());
    }

    @Test
    public void shouldCountVowels() {
        var instance =
                Module.build(new File("src/test/resources/compiled/count_vowels.rs.wasm"))
                        .instantiate();
        var alloc = instance.getExport("alloc");
        var dealloc = instance.getExport("dealloc");
        var countVowels = instance.getExport("count_vowels");
        var memory = instance.getMemory();
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
        var instance =
                Module.build(new File("src/test/resources/compiled/basic.c.wasm")).instantiate();
        var run = instance.getExport("run");
        var result = run.apply()[0];
        assertEquals(42, result.asInt());
    }

    // @Test
    // public void shouldRunComplexFunction() {
    // // check with: wasmtime src/test/resources/wasm/complex.c.wasm --invoke run
    // var instance =
    // Module.build("src/test/resources/wasm/complex.c.wasm").instantiate();
    // var run = instance.getExport("run");
    // var result = run.apply();
    // assertEquals(-679, result.asInt());
    // }

    // @Test
    // public void shouldRunMemoryProgramInC() {
    // // check with: wasmtime src/test/resources/wasm/memory.c.wasm --invoke run
    // var instance =
    // Module.build("src/test/resources/wasm/memory.c.wasm").instantiate();
    // var run = instance.getExport("run");
    // var result = run.apply();
    // assertEquals(11, result.asInt());
    // }

    @Test
    public void shouldWorkWithMemoryOps() {
        var instance =
                Module.build(new File("src/test/resources/compiled/memory.wat.wasm")).instantiate();
        var run = instance.getExport("run32");
        var results = run.apply(Value.i32(42));
        var result = results[0];
        assertEquals(42, result.asInt());

        result = run.apply(Value.i32(Integer.MAX_VALUE))[0];
        assertEquals(Integer.MAX_VALUE, result.asInt());

        result = run.apply(Value.i32(Integer.MIN_VALUE))[0];
        assertEquals(Integer.MIN_VALUE, result.asInt());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(42))[0];
        assertEquals(42L, result.asLong());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(Long.MIN_VALUE))[0];
        assertEquals(Long.MIN_VALUE, result.asLong());

        run = instance.getExport("run64");
        result = run.apply(Value.i64(Long.MAX_VALUE))[0];
        assertEquals(Long.MAX_VALUE, result.asLong());
    }

    @Test
    public void shouldRunKitchenSink() {
        // check with: wasmtime src/test/resources/wasm/kitchensink.wat.wasm --invoke
        // run 100
        var instance =
                Module.build(new File("src/test/resources/compiled/kitchensink.wat.wasm"))
                        .instantiate();

        var run = instance.getExport("run");
        assertEquals(6, run.apply(Value.i32(100))[0].asInt());
    }

    @Test
    public void shouldRunWasiModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.wat.wasm
        var fakeStdout = new MockPrintStream();
        var wasi = new WasiP1(WasiOptions.builder().setStdout(fakeStdout));
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/hello-wasi.wat.wasm"))
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply();
        assertEquals(fakeStdout.getOutput(), "hello world\n");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!\n";
        var stdout = new MockPrintStream();
        var wasi = new WasiP1(WasiOptions.builder().setStdout(stdout));
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/hello-wasi.rs.wasm"))
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply(); // prints Hello, World!
        assertEquals(expected, stdout.getOutput());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes());
        var wasiOpts = WasiOptions.builder().setStdout(System.out).setStdin(fakeStdin);
        var wasi = new WasiP1(wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/greet-wasi.rs.wasm"))
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply();
    }

    // @Test
    // public void shouldOperateMemoryOps() {
    // // check with: wasmtime src/test/resources/wasm/memories.wat.wasm --invoke
    // run 100
    // var instance =
    // Module.build("src/test/resources/wasm/memories.wat.wasm").instantiate();
    // var run = instance.getExport("run");
    // assertEquals(-25438, run.apply(Value.i32(100)).asInt());
    // }
}
