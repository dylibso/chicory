package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.wasi.WasiOptions;
import com.dylibso.chicory.runtime.wasi.WasiPreview1;
import java.io.ByteArrayInputStream;
import java.io.File;
import org.junit.jupiter.api.Test;

public class WasiPreview1Test {
    private final Logger logger = new SystemLogger();

    @Test
    public void shouldRunWasiModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.wat.wasm
        var fakeStdout = new MockPrintStream();
        var wasi =
                new WasiPreview1(this.logger, WasiOptions.builder().withStdout(fakeStdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.builder(new File("src/test/resources/compiled/hello-wasi.wat.wasm"))
                        .build()
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply();
        assertEquals(fakeStdout.getOutput().strip(), "hello world");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!";
        var stdout = new MockPrintStream();
        var wasi = new WasiPreview1(this.logger, WasiOptions.builder().withStdout(stdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.builder(new File("src/test/resources/compiled/hello-wasi.rs.wasm"))
                        .build()
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply(); // prints Hello, World!
        assertEquals(expected, stdout.getOutput().strip());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes());
        var wasiOpts = WasiOptions.builder().withStdout(System.out).withStdin(fakeStdin).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.builder(new File("src/test/resources/compiled/greet-wasi.rs.wasm"))
                        .build()
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply();
    }
}
