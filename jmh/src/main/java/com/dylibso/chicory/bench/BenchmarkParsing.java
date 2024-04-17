package com.dylibso.chicory.bench;

import com.dylibso.chicory.runtime.Module;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
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
public class BenchmarkParsing {

    @Param({
        "wasm-corpus/src/test/resources/compiled/basic.c.wasm",
        //        "wasm-corpus/src/test/resources/compiled/javy-demo.js.javy.wasm"
    })
    private String fileName;

    byte[] memoryMappedFile;

    @Setup
    public void setup() throws IOException {
        memoryMappedFile = FileUtils.readFileToByteArray(new File(fileName));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void benchmark(Blackhole bh) {
        bh.consume(Module.builder(memoryMappedFile).build());
    }
}