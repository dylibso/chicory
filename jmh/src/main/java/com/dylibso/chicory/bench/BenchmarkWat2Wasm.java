package com.dylibso.chicory.bench;

import com.dylibso.chicory.corpus.WatGenerator;
import com.dylibso.chicory.wabt.Wat2Wasm;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class BenchmarkWat2Wasm {

    String wat;

    @Setup
    public void setup() {
        wat = WatGenerator.bigWat(1000, 10);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(8)
    public void benchmarkAot(Blackhole bh) {
        bh.consume(Wat2Wasm.parse(wat));
    }
}
