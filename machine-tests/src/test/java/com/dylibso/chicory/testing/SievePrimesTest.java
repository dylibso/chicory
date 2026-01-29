package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class SievePrimesTest {

    private static final WasmModule module =
            Parser.parse(CorpusResources.getResource("compiled/sieve-primes.rs.wasm"));

    private static Instance newInstance(Memory memory) {
        return Instance.builder(module)
                .withImportValues(
                        ImportValues.builder()
                                .addMemory(new ImportMemory("env", "memory", memory))
                                .build())
                .build();
    }

    private static Thread newThread(Instance parent) {
        var memory = parent.imports().memory(0).memory();
        var stackSize = parent.exports().global("__stack_pointer").getValue();
        var tlsSize = parent.exports().global("__tls_size").getValue();
        var tlsAlign = parent.exports().global("__tls_align").getValue();

        var stackPtr = parent.export("__malloc").apply(stackSize, 16)[0] + stackSize;
        var child = newInstance(memory);
        child.exports().global("__stack_pointer").setValue(stackPtr);
        var tlsPtr = child.export("__malloc").apply(tlsSize, tlsAlign);
        child.export("__wasm_init_tls").apply(tlsPtr);
        return new Thread(
                () -> {
                    child.export("register_thread").apply();
                });
    }

    @Test
    @Timeout(60)
    public void sievePrimesTest() throws Exception {

        // Shared memory - needs enough space for sieve (64KB offset + sieve data)
        var memory = new ByteArrayMemory(new MemoryLimits(20, 100, true));

        var instance = newInstance(memory);
        var parallelSieve = instance.export("parallel_sieve_bitset");

        // Spawn worker threads
        int numWorkers = 4;
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            Thread t = newThread(instance);
            workers.add(t);
            t.start();
        }

        // Run the sieve
        int limit = 1_000_000;
        long[] result = parallelSieve.apply(limit);
        int primeCount = (int) result[0];

        assertEquals(78498, primeCount, "Expected 78498 primes up to 1,000,000");

        instance.export("shutdown").apply();
        for (var thread : workers) {
            thread.join();
        }
    }
}
