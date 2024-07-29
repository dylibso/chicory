package com.dylibso.chicory.bench;

import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class BenchmarkParsing {

    @Param({
        "wasm-corpus/src/main/resources/compiled/basic.c.wasm",
        "wasm-corpus/src/main/resources/compiled/javy-demo.js.javy.wasm"
    })
    private String fileName;

    byte[] memoryMappedFile;

    @Setup
    public void setup() throws IOException {
        memoryMappedFile = Files.readAllBytes(Paths.get(fileName));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmark(Blackhole bh) {
        bh.consume(WasmModule.builder(memoryMappedFile).build());
    }
}
