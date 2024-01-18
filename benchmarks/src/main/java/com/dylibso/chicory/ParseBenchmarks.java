package com.dylibso.chicory;

import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.wasi.WasiOptionsBuilder;
import com.dylibso.chicory.runtime.wasi.WasiPreview1;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class ParseBenchmarks {
    @State(Scope.Thread)
    public static class MyState {
        private Module zigMod;
        private HostImports imports;

        @Setup(Level.Trial)
        public void doSetup() {
            this.zigMod = Module.build(new File("src/main/resources/compiled/zig.wasm"));
            var wasi = new WasiPreview1(new WasiOptionsBuilder().inheritSystem().build());
            this.imports = new HostImports(wasi.toHostFunctions());
        }
    }

    @Benchmark
    public void parseZig() {
        var m = Module.build(new File("src/main/resources/compiled/zig.wasm"));
    }

    public void instantiateZig(MyState state) {
        var i = state.zigMod.instantiate(state.imports);
    }
}
