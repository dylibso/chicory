package com.dylibso.chicory.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.ByteArrayInputStream;
import java.util.List;
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
        Instance.builder(
                        Module.builder(
                                        Thread.currentThread()
                                                .getContextClassLoader()
                                                .getResourceAsStream(
                                                        "compiled/hello-wasi.wat.wasm"))
                                .build())
                .withHostImports(imports)
                .build();
        assertEquals(fakeStdout.output().strip(), "hello world");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!";
        var stdout = new MockPrintStream();
        var wasi = new WasiPreview1(this.logger, WasiOptions.builder().withStdout(stdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        Instance.builder(
                        Module.builder(
                                        Thread.currentThread()
                                                .getContextClassLoader()
                                                .getResourceAsStream("compiled/hello-wasi.rs.wasm"))
                                .build())
                .withHostImports(imports)
                .build(); // run _start and prints Hello, World!
        assertEquals(expected, stdout.output().strip());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes());
        var wasiOpts = WasiOptions.builder().withStdout(System.out).withStdin(fakeStdin).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        Instance.builder(
                        Module.builder(
                                        Thread.currentThread()
                                                .getContextClassLoader()
                                                .getResourceAsStream("compiled/greet-wasi.rs.wasm"))
                                .build())
                .withHostImports(imports)
                .build();
    }

    @Test
    public void shouldRunWasiDemoJavyModule() {
        // check with: echo "{ \"n\": 2, \"bar\": \"baz\"}" | wasmtime
        // wasi/src/test/resources/compiled/javy-demo.js.wasm
        var fakeStdin = new ByteArrayInputStream("{ \"n\": 2, \"bar\": \"baz\" }".getBytes());
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).withStdin(fakeStdin).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        Instance.builder(
                        Module.builder(
                                        Thread.currentThread()
                                                .getContextClassLoader()
                                                .getResourceAsStream(
                                                        "compiled/javy-demo.js.javy.wasm"))
                                .build())
                .withHostImports(imports)
                .build();

        assertEquals(fakeStdout.output(), "{\"foo\":3,\"newBar\":\"baz!\"}");
    }

    @Test
    public void shouldRunTinyGoModule() {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module =
                Module.builder(
                                Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream("compiled/sum.go.tiny.wasm"))
                        .build();
        var instance = Instance.builder(module).withHostImports(imports).build();
        var sum = instance.export("add");
        var result = sum.apply(Value.i32(20), Value.i32(22))[0];

        assertEquals(result.asInt(), 42);
    }

    @Test
    public void shouldRunWasiGoModule() {
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module =
                Module.builder(
                                Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream("compiled/main.go.wasm"))
                        .build();
        var wrapped =
                assertThrows(
                        WASMMachineException.class,
                        () -> Instance.builder(module).withHostImports(imports).build());
        WasiExitException exit = (WasiExitException) wrapped.getCause();
        assertEquals(0, exit.exitCode());
        assertEquals("Hello, WebAssembly!\n", fakeStdout.output());
    }

    @Test
    public void shouldRunWasiDemoDotnetModule() throws Exception {
        var fakeStdout = new MockPrintStream();
        var wasiOpts =
                WasiOptions.builder()
                        .withStdout(fakeStdout)
                        // Fix for "[MONO] critical: /__w/1/s/src/mono/mono/eglib/gpath.c:134:
                        // assertion 'filename != NULL' failed"
                        // https://jflower.co.uk/running-net-8-on-cloudflare-workers/
                        .withArguments(List.of(""))
                        .build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());

        var module =
                Module.builder(
                                Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream("compiled/basic.dotnet.wasm"))
                        .build();
        Instance.builder(module).withHostImports(imports).build();

        assertEquals("Hello, Wasi Console!\n", fakeStdout.output());
    }
}
