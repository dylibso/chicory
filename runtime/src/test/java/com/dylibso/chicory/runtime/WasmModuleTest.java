package com.dylibso.chicory.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.TagType;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class WasmModuleTest {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(WasmModuleTest.class.getResourceAsStream("/" + fileName));
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
                        FunctionType.of(List.of(ValType.I32, ValType.I32), List.of()),
                        (Instance instance, long... args) -> { // decompiled is: console_log(13, 0);
                            Memory memory = instance.memory();
                            int len = (int) args[0];
                            int offset = (int) args[1];
                            var message = memory.readString(offset, len);

                            if (expected.equals(message)) {
                                count.incrementAndGet();
                            }

                            return null;
                        });
        var funcs = new HostFunction[] {func};
        var instance =
                Instance.builder(loadModule("compiled/host-function.wat.wasm"))
                        .withImportValues(ImportValues.builder().addFunction(funcs).build())
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
                        FunctionType.of(List.of(ValType.I32), List.of()),
                        (Instance instance, long... args) -> {
                            var val = args[0];

                            if (val == 42L) {
                                count.incrementAndGet();
                            }

                            return null;
                        });
        var funcs = new HostFunction[] {func};
        Instance.builder(loadModule("compiled/start.wat.wasm"))
                .withImportValues(ImportValues.builder().addFunction(funcs).build())
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
    public void shouldSupportMemoryLimitsOverride() {
        var instance =
                Instance.builder(loadModule("compiled/count_vowels.rs.wasm"))
                        .withMemoryLimits(new MemoryLimits(17, 17))
                        .build();
        assertThrows(TrapException.class, () -> instance.export("alloc").apply(Memory.PAGE_SIZE));
    }

    @Test
    public void shouldSupportMemoryFactoryOverride() {
        AtomicBoolean memoryCreated = new AtomicBoolean();
        memoryCreated.set(false);
        Instance.builder(loadModule("compiled/count_vowels.rs.wasm"))
                .withMemoryFactory(
                        limits -> {
                            memoryCreated.set(true);
                            return new ByteBufferMemory(limits);
                        })
                .build();
        assertEquals(true, memoryCreated.get());
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
        var instance =
                Instance.builder(loadModule("compiled/mixed-imports.wat.wasm"))
                        .withImportValues(hostImports)
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
    public void shouldEasilyObtainExportedEntities() {
        var instance = Instance.builder(loadModule("compiled/exports.wat.wasm")).build();

        assertNotNull(instance.exports().memory("mem").pages());
        assertNotNull(instance.exports().table("tab").size());
        assertNotNull(instance.exports().global("glob1").getValue());
        assertNotNull(instance.exports().function("get-1").apply());
    }

    @Test
    public void shouldThrowOnInvalidExports() {
        var instance = Instance.builder(loadModule("compiled/exports.wat.wasm")).build();

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

        var instance =
                Instance.builder(loadModule("compiled/alias-imports1.wat.wasm"))
                        .withImportValues(imports)
                        .build();

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

        var instance =
                Instance.builder(loadModule("compiled/alias-imports2.wat.wasm"))
                        .withImportValues(imports)
                        .build();

        instance.exports().function("log-i32").apply(0);
        assertTrue(loggedI32.get());
        assertFalse(loggedI64.get());
        instance.exports().function("log-i64").apply(0);
        assertTrue(loggedI32.get());
        assertTrue(loggedI64.get());
    }

    @Test
    public void shouldResolveMultipleAliasesByTypeForAllImports() {
        var module = loadModule("compiled/alias-imports3.wat.wasm");
        var globalI32 = new ImportGlobal("env", "global", new GlobalInstance(Value.i32(123)));
        var globalI64 = new ImportGlobal("env", "global", new GlobalInstance(Value.i64(124)));
        var tableFuncref =
                new ImportTable(
                        "env",
                        "table",
                        new TableInstance(new Table(ValType.FuncRef, new TableLimits(1))));
        var tableExternref =
                new ImportTable(
                        "env",
                        "table",
                        new TableInstance(new Table(ValType.ExternRef, new TableLimits(2))));
        var tagI32 =
                new ImportTag(
                        "env",
                        "tag",
                        new TagInstance(new TagType((byte) 0, 0), module.typeSection().getType(0)));
        var tagI64 =
                new ImportTag(
                        "env",
                        "tag",
                        new TagInstance(new TagType((byte) 0, 1), module.typeSection().getType(1)));

        var imports =
                ImportValues.builder()
                        .addGlobal(globalI64)
                        .addGlobal(globalI32)
                        .addTable(tableExternref)
                        .addTable(tableFuncref)
                        .addTag(tagI64)
                        .addTag(tagI32)
                        .build();

        var instance =
                Instance.builder(loadModule("compiled/alias-imports3.wat.wasm"))
                        .withImportValues(imports)
                        .build();

        assertEquals(123L, instance.imports().global(0).instance().getValue());
        assertEquals(124L, instance.imports().global(1).instance().getValue());
        assertEquals(ValType.FuncRef, instance.imports().table(0).table().elementType());
        assertEquals(ValType.ExternRef, instance.imports().table(1).table().elementType());
        assertEquals(0, instance.imports().tag(0).tag().tagType().typeIdx());
        assertEquals(1, instance.imports().tag(1).tag().tagType().typeIdx());
    }

    @Test
    public void correctlyReturnAllLabels() {
        // Arrange
        var operands = new long[] {64, 2, 0, 0, 0, 0, 0, 0};

        // Act
        var result = CatchOpCode.allLabels(operands);

        // Assert
        assertEquals(2, result.size());
        assertEquals(0, result.get(0));
        assertEquals(0, result.get(1));
    }
}
