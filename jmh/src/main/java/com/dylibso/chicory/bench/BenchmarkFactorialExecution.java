package com.dylibso.chicory.bench;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class BenchmarkFactorialExecution {

    private static final File ITERFACT =
            new File("wasm-corpus/src/main/resources/compiled/iterfact.wat.wasm");

    @Param({"5", "1000"})
    private int input;

    Module aotModule;

    ExportFunction iterFactInt;
    ExportFunction iterFactAot;

    @Setup
    public void setup() {
        var factorialInt = Module.builder(ITERFACT).build().instantiate();
        iterFactInt = factorialInt.export("iterFact");

        aotModule = Module.builder(ITERFACT).withMachineFactory(AotMachine::new).build();
        var factorialAot = aotModule.instantiate();
        iterFactAot = factorialAot.export("iterFact");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkInt(Blackhole bh) {
        bh.consume(iterFactInt.apply(Value.i32(input)));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkAot(Blackhole bh) {
        bh.consume(iterFactAot.apply(Value.i32(input)));
    }
}
