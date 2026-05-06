package com.dylibso.chicory.bench;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
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

// Measures dispatch overhead for large wasm modules (2000 functions).
// Compare with/without the HugeMethodLimit-aware chunking:
//   java -Dchicory.hugeMethodLimit=1000000 -jar benchmarks.jar BenchmarkDispatchChunkSize  (no
// limit)
//   java -jar benchmarks.jar BenchmarkDispatchChunkSize                                    (default
// 8KB)
//
// Results (JDK 25, Apple M3 Max):
//   Benchmark                            (targetFunc)  Mode  Cnt  Score    Units
//   -- hugeMethodLimit=1000000 (chunks of 1024, exceeds C2 HugeMethodLimit) --
//   BenchmarkDispatchChunkSize.dispatch             0  avgt    5  0.033   us/op
//   BenchmarkDispatchChunkSize.dispatch           999  avgt    5  0.034   us/op
//   BenchmarkDispatchChunkSize.dispatch          1999  avgt    5  0.035   us/op
//   -- default (chunks of 128, under C2 HugeMethodLimit) --
//   BenchmarkDispatchChunkSize.dispatch             0  avgt    5  0.004   us/op
//   BenchmarkDispatchChunkSize.dispatch           999  avgt    5  0.004   us/op
//   BenchmarkDispatchChunkSize.dispatch          1999  avgt    5  0.004   us/op
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
public class BenchmarkDispatchChunkSize {

    private static final int NUM_FUNCTIONS = 2000;

    @Param({"0", "999", "1999"})
    private int targetFunc;

    private ExportFunction exportFunc;

    @Setup
    public void setup() {
        StringBuilder wat = new StringBuilder();
        wat.append("(module\n");
        for (int i = 0; i < NUM_FUNCTIONS; i++) {
            wat.append("  (func $f").append(i);
            wat.append(" (export \"f").append(i).append("\")");
            wat.append(" (param i32) (result i32)\n");
            wat.append("    local.get 0\n");
            wat.append("    i32.const ").append(i + 1).append("\n");
            wat.append("    i32.add)\n");
        }
        wat.append(")\n");

        byte[] wasm = Wat2Wasm.parse(wat.toString());
        Instance instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();
        exportFunc = instance.export("f" + targetFunc);
    }

    @Benchmark
    public void dispatch(Blackhole bh) {
        bh.consume(exportFunc.apply(42));
    }
}
