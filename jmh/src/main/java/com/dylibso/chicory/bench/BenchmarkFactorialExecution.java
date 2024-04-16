package com.dylibso.chicory.bench;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import java.io.IOException;
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
// @Warmup(iterations = 2)
// @Measurement(iterations = 5)
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class BenchmarkFactorialExecution {

    @Param({
        "5",
        //            "1000"
    })
    private int input;

    ExportFunction iterFact;

    @Setup
    public void setup() throws IOException {
        var factorial =
                Module.builder(
                                new File(
                                        "wasm-corpus/src/test/resources/compiled/iterfact.wat.wasm"))
                        .build()
                        .instantiate();
        iterFact = factorial.export("iterFact");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmark(Blackhole bh) {
        bh.consume(iterFact.apply(Value.i32(input)));
    }
}
