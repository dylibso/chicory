package com.dylibso.chicory;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class RuntimeBenchmarks {
    @State(Scope.Thread)
    public static class MyState {
        private Instance fibModule;
        private ExportFunction fibonacci;

        @Setup(Level.Trial)
        public void doSetup() {
            this.fibModule =
                    Module.build(new File("src/main/resources/compiled/fib.wat.wasm"))
                            .instantiate();
            this.fibonacci = fibModule.getExport("fib");
        }
    }

    @Benchmark
    public void fibonacci(MyState state) {
        var result = state.fibonacci.apply(Value.i32(20));
        // System.out.println(result);
    }
}
