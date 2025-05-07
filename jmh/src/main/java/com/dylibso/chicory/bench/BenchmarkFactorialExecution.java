package com.dylibso.chicory.bench;

import com.dylibso.chicory.compiler.CompilerMachine;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
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

    ExportFunction iterFactInt;
    ExportFunction iterFactCompiled;

    @Setup
    public void setup() {
        var factorialInt = Instance.builder(Parser.parse(ITERFACT)).build();
        iterFactInt = factorialInt.export("iterFact");

        var factorialCompiled =
                Instance.builder(Parser.parse(ITERFACT))
                        .withMachineFactory(CompilerMachine::new)
                        .build();
        iterFactCompiled = factorialCompiled.export("iterFact");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkInt(Blackhole bh) {
        bh.consume(iterFactInt.apply(input));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmarkCompiled(Blackhole bh) {
        bh.consume(iterFactCompiled.apply(input));
    }
}
