package com.dylibso.chicory.bench;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class BenchmarkSievePrimes {

    private static final File SIEVE_PRIMES =
            new File("wasm-corpus/src/main/resources/compiled/sieve-primes.rs.wasm");

    private static final WasmModule module = Parser.parse(SIEVE_PRIMES);

    @Param("4")
    private int numWorkers;

    @Param("1000000")
    private int limit;

    @Param({"ByteArrayMemory", "ByteBufferMemory"})
    private String memoryType;

    @Param("compiled")
    private String machineType;

    private Instance instance;
    private ExportFunction sieve;
    private List<Thread> workers;

    private Instance newInstance(Memory memory) {
        var builder =
                Instance.builder(module)
                        .withImportValues(
                                ImportValues.builder()
                                        .addMemory(new ImportMemory("env", "memory", memory))
                                        .build());
        if (machineType.equals("compiled")) {
            builder.withMachineFactory(MachineFactoryCompiler::compile);
        }
        return builder.build();
    }

    private Thread newThread(Instance parent) {
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

    private Memory createMemory() {
        var limits = new MemoryLimits(20, 100, true);
        return memoryType.equals("ByteBufferMemory")
                ? new ByteBufferMemory(limits)
                : new ByteArrayMemory(limits);
    }

    @Setup(Level.Trial)
    public void setup() {
        var memory = createMemory();
        instance = newInstance(memory);
        sieve = instance.export("parallel_sieve_bitset");
        workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            Thread t = newThread(instance);
            workers.add(t);
            t.start();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        instance.export("shutdown").apply();
        for (var thread : workers) {
            thread.join();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmark(Blackhole bh) {
        bh.consume(sieve.apply(limit));
    }
}
