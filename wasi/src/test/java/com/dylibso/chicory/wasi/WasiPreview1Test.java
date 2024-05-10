package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Files.copyDirectory;
import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
        var module = Module.builder("compiled/hello-wasi.wat.wasm").build();
        module.withHostImports(imports).instantiate();
        assertEquals(fakeStdout.output().strip(), "hello world");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!";
        var stdout = new MockPrintStream();
        var wasi = new WasiPreview1(this.logger, WasiOptions.builder().withStdout(stdout).build());
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/hello-wasi.rs.wasm").build();
        module.withHostImports(imports).instantiate(); // run _start and prints Hello, World!
        assertEquals(expected, stdout.output().strip());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes());
        var wasiOpts = WasiOptions.builder().withStdout(System.out).withStdin(fakeStdin).build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/greet-wasi.rs.wasm").build();
        module.withHostImports(imports).instantiate();
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
        var module = Module.builder("compiled/javy-demo.js.javy.wasm").build();
        module.withHostImports(imports).instantiate();

        assertEquals(fakeStdout.output(), "{\"foo\":3,\"newBar\":\"baz!\"}");
    }

    @Test
    public void shouldRunTinyGoModule() {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = new WasiPreview1(this.logger, wasiOpts);
        var imports = new HostImports(wasi.toHostFunctions());
        var module = Module.builder("compiled/sum.go.tiny.wasm").build();
        var instance = module.withHostImports(imports).instantiate();
        var sum = instance.export("add");
        var result = sum.apply(Value.i32(20), Value.i32(22))[0];

        assertEquals(result.asInt(), 42);
    }

    private Value[] wasiResult(WasiErrno errno) {
        if (errno != WasiErrno.ESUCCESS) {
            logger.info("result = " + errno.name());
        }
        return new Value[] {Value.i32(errno.ordinal())};
    }

    @Test
    public void shouldRunWat2Wasm() throws Exception {
        var module = Module.builder("compiled/wat2wasm").build();

        try (FileInputStream fis = new FileInputStream("../wasm-corpus/src/test/resources/wat/iterfact.wat");
                FileSystem fs =
                     Jimfs.newFileSystem(
                             Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

            var wasiOpts = WasiOptions.builder();

            wasiOpts.inheritSystem();
            var stdoutStream = new ByteArrayOutputStream();
            wasiOpts.withStdout(stdoutStream);

            Path target = fs.getPath("tmp");
            java.nio.file.Files.createDirectory(target);
            Path path = target.resolve("file.wat");
            copy(fis, path, StandardCopyOption.REPLACE_EXISTING);
            wasiOpts.withDirectory(target.toString(), target);

            wasiOpts.withArguments(
                    List.of("wat2wasm", path.toString(), "--output=-")
            );

            var wasi = new WasiPreview1(this.logger, wasiOpts.build());
            var imports = new HostImports(wasi.toHostFunctions());

            module.withHostImports(imports).instantiate();

            assertNotNull(stdoutStream.toByteArray());
        }
    }
}
