package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.WatGenerator;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class LockStepMemoryAccessTest {

    public interface Workload {
        void run(Function<MemoryLimits, Memory> memoryFactory, String name);
    }

    public LockStepMemory[] assertHasSameMemoryAccess(String name1, String name2, Workload workload)
            throws IOException {
        var pair =
                LockStepMemory.create(
                        name1,
                        new ByteArrayMemory(new MemoryLimits(1024, 65536)),
                        name2,
                        new ByteArrayMemory(new MemoryLimits(1024, 65536)));
        new Thread(
                        () -> {
                            workload.run((x) -> pair[1], name2);
                        })
                .start();
        workload.run((x) -> pair[0], name1);
        return pair;
    }

    private void testMemoryWat(Function<MemoryLimits, Memory> memoryFactory, String name) {
        var module = Parser.parse(getClass().getResourceAsStream("/compiled/memory.wat.wasm"));
        Instance.Builder builder = Instance.builder(module).withMemoryFactory(memoryFactory);
        if (name.startsWith("aot")) {
            builder.withMachineFactory(MachineFactoryCompiler::compile);
        }
        var instance = builder.build();
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
    public void twoInterpretersHaveSameMemoryAccess() throws IOException {
        var pair = assertHasSameMemoryAccess("interpreter 1", "interpreter 2", this::testMemoryWat);
        assertEquals(14, pair[0].eventCounter());
    }

    @Test
    public void twoAotsHaveSameMemoryAccess() throws IOException {
        var pair = assertHasSameMemoryAccess("aot 1", "aot 2", this::testMemoryWat);
        assertEquals(14, pair[0].eventCounter());
    }

    @Test
    public void aotAndInterpretersHaveSameMemoryAccess() throws IOException {
        var pair = assertHasSameMemoryAccess("interpreter", "aot", this::testMemoryWat);
        assertEquals(14, pair[0].eventCounter());
    }

    private void testWat2Wasm(Function<MemoryLimits, Memory> memoryFactory, String name) {
        var options = Wat2Wasm.options().withMemoryFactory(memoryFactory);
        if (name.startsWith("aot")) {
            options.withExecutionType(Wat2Wasm.ExecutionType.AOT);
        } else {
            options.withExecutionType(Wat2Wasm.ExecutionType.INTERPRETED);
        }
        var wat = WatGenerator.bigWat(10_000, 0);
        Wat2Wasm.parse(wat, options);
    }

    @Test
    public void wat2wasmAotAndInterpretersHaveSameMemoryAccess() throws IOException {
        var pair = assertHasSameMemoryAccess("aot", "interpreter", this::testWat2Wasm);
        assertEquals(14, pair[0].eventCounter());
    }
}
