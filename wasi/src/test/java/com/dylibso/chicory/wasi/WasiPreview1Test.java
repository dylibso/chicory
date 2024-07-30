package com.dylibso.chicory.wasi;

import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        var module =
                Module.builder("compiled/hello-wasi.wat.wasm").withHostImports(imports).build();
        module.instantiate();
        assertEquals(fakeStdout.output().strip(), "hello world");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!";
        var stdout = new MockPrintStream();
        var wasi = new WasiPreview1(this.logger, WasiOptions.builder().withStdout(stdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/hello-wasi.rs.wasm").withHostImports(imports).build();
        module.instantiate(); // run _start and prints Hello, World!
        assertEquals(expected, stdout.output().strip());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes());
        var wasiOpts = WasiOptions.builder().withStdout(System.out).withStdin(fakeStdin).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/greet-wasi.rs.wasm").withHostImports(imports).build();
        module.instantiate();
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
        var module =
                Module.builder("compiled/javy-demo.js.javy.wasm").withHostImports(imports).build();
        module.instantiate();

        assertEquals(fakeStdout.output(), "{\"foo\":3,\"newBar\":\"baz!\"}");
    }

    @Test
    public void shouldRunTinyGoModule() {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/sum.go.tiny.wasm").withHostImports(imports).build();
        var instance = module.instantiate();
        var sum = instance.export("add");
        var result = sum.apply(Value.i32(20), Value.i32(22))[0];

        assertEquals(result.asInt(), 42);
    }

    @Test
    public void shouldRunPythonModule() throws Exception {
        // implementation of this tutorial:
        var fakeStdout = new MockPrintStream();
        var fakeStderr = new MockPrintStream();
        FileSystem fs =
                Jimfs.newFileSystem(
                        Configuration.unix().toBuilder().setAttributeViews("unix").build());
        // --mapdir /::$PWD \
        Path inputFolder = fs.getPath("/");

        // cannot get inline python to run
        // but works when loading a file ...
        // -- -c "import sys; from pprint import pprint as pp; \
        //         pp(sys.path); pp(sys.platform)"
        var script = "print(\"hello python!\")";

        FileWriter fileWriter = new FileWriter(new File("/tmp/try-python-wasm").toPath().resolve("test.py").toFile());
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(script);
        printWriter.flush();
        printWriter.close();

        Files.copyDirectory(new File("/tmp/try-python-wasm").toPath(), inputFolder);

        var wasiOpts =
                WasiOptions.builder()
                        .withDirectory(inputFolder.toString(), inputFolder)
                        .withArguments(List.of("-c", "test.py"))
                        .withStdout(fakeStdout)
                        .withStderr(fakeStderr)
                        .build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var file = new File("/tmp/try-python-wasm/bin/python-3.11.1.wasm");

        var module = Module.builder(file).withHostImports(imports).build();
        module.instantiate();

        assertEquals("hello python!\n", fakeStdout.output());
    }
}
