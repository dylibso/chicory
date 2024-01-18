package com.dylibso.chicory;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 3)
public class ParseBenchmarks {
    @State(Scope.Thread)
    public static class MyState {
        private Module zigMod;
        ///private WasiP1 wasi;

        @Setup(Level.Trial)
        public void doSetup() {
            this.zigMod =
                    Module.build(new File("src/main/resources/compiled/zig.wasm"));
        }
    }

    @Benchmark
    public void parseZig() {
        var m = Module.build(new File("src/main/resources/compiled/zig.wasm"));
    }

    public void instantiateZig(MyState state) {
        var i = state.zigMod.instantiate();
    }
}
