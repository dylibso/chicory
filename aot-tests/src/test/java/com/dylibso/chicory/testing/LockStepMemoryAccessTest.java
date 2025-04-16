package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
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
                        new ByteArrayMemory(new MemoryLimits(1)),
                        name2,
                        new ByteArrayMemory(new MemoryLimits(1)));
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
            builder.withMachineFactory(AotMachine::new);
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
    public void AotAndInterpretersHaveSameMemoryAccess() throws IOException {
        var pair = assertHasSameMemoryAccess("interpreter", "aot", this::testMemoryWat);
        assertEquals(14, pair[0].eventCounter());
    }
}
