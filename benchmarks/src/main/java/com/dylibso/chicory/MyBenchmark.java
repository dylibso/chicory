package com.dylibso.chicory;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class MyBenchmark {
    @State(Scope.Thread)
    public static class MyState {
        private Instance factModule;
        private ExportFunction factorial;

        private Instance fibModule;
        private ExportFunction fibonacci;

        @Setup(Level.Trial)
        public void doSetup() {
            this.factModule =
                    Module.build(new File("src/main/resources/compiled/iterfact.wat.wasm"))
                            .instantiate();
            this.factorial = factModule.getExport("iterFact");
            this.fibModule =
                    Module.build(new File("src/main/resources/compiled/fib.wat.wasm"))
                            .instantiate();
            this.fibonacci = fibModule.getExport("fib");
        }
    }

    @Benchmark
    public void factorial(MyState state) {
        var result = state.factorial.apply(Value.i32(300));
        // System.out.println(result);
    }

    @Benchmark
    public void fibonacci(MyState state) {
        var result = state.fibonacci.apply(Value.i32(10));
        // System.out.println(result);
    }
}
