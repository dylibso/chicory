package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.dylibso.chicory.runtime.wasi.WasiOptions;
import com.dylibso.chicory.runtime.wasi.WasiPreview1;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class MockPrintStream extends PrintStream {
    private ByteArrayOutputStream baos;

    public MockPrintStream() {
        super(new ByteArrayOutputStream());
        this.baos = (ByteArrayOutputStream) this.out;
    }

    @Override
    public void println(String s) {
        super.println(s);
    }

    public String getOutput() {
        return baos.toString();
    }
}

public class WasiPreview1Test {

    @Test
    public void shouldRunWasiModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.wat.wasm
        var fakeStdout = new MockPrintStream();
        var wasi = new WasiPreview1(WasiOptions.builder().withStdout(fakeStdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/hello-wasi.wat.wasm"))
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
        var wasi = new WasiPreview1(WasiOptions.builder().withStdout(stdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/hello-wasi.rs.wasm"))
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
        var wasi = new WasiPreview1(wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var instance =
                Module.build(new File("src/test/resources/compiled/greet-wasi.rs.wasm"))
                        .instantiate(imports);
        var run = instance.getExport("_start");
        run.apply();
    }
}
