package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Files.copyDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createSymbolicLink;
import static java.nio.file.Files.writeString;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public class WasiPreview1Test {
    private final Logger logger = new SystemLogger();

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(WasiPreview1Test.class.getResourceAsStream("/" + fileName));
    }

    @Test
    public void shouldRunWasiModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.wat.wasm
        var fakeStdout = new MockPrintStream();
        var wasi =
                WasiPreview1.builder()
                        .withLogger(this.logger)
                        .withOptions(WasiOptions.builder().withStdout(fakeStdout).build())
                        .build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        Instance.builder(loadModule("compiled/hello-wasi.wat.wasm"))
                .withImportValues(imports)
                .build();
        assertEquals(fakeStdout.output().strip(), "hello world");
    }

    @Test
    public void shouldRunWasiRustModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.rs.wasm
        var expected = "Hello, World!";
        var stdout = new MockPrintStream();
        var wasi =
                WasiPreview1.builder()
                        .withLogger(this.logger)
                        .withOptions(WasiOptions.builder().withStdout(stdout).build())
                        .build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        Instance.builder(loadModule("compiled/hello-wasi.rs.wasm"))
                .withImportValues(imports)
                .build(); // run _start and prints Hello, World!
        assertEquals(expected, stdout.output().strip());
    }

    @Test
    public void shouldRunWasiGreetRustModule() {
        // check with: wasmtime src/test/resources/compiled/greet-wasi.rs.wasm
        var fakeStdin = new ByteArrayInputStream("Benjamin".getBytes(UTF_8));
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).withStdin(fakeStdin).build();
        var wasi = WasiPreview1.builder().withLogger(this.logger).withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        Instance.builder(loadModule("compiled/greet-wasi.rs.wasm"))
                .withImportValues(imports)
                .build();
        assertEquals(fakeStdout.output().strip(), "Hello, Benjamin!");
    }

    @Test
    public void shouldRunWasiDemoJavyModule() {
        // check with: echo "{ \"n\": 2, \"bar\": \"baz\"}" | wasmtime
        // wasi/src/test/resources/compiled/javy-demo.js.wasm
        var fakeStdin = new ByteArrayInputStream("{ \"n\": 2, \"bar\": \"baz\" }".getBytes(UTF_8));
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).withStdin(fakeStdin).build();
        var wasi = WasiPreview1.builder().withLogger(this.logger).withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        Instance.builder(loadModule("compiled/javy-demo.js.javy.wasm"))
                .withImportValues(imports)
                .build();

        assertEquals(fakeStdout.output(), "{\"foo\":3,\"newBar\":\"baz!\"}");
    }

    @Test
    public void shouldUseQuickJsProvider() {
        ByteArrayInputStream stdin = new ByteArrayInputStream("".getBytes(UTF_8));
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();

        var wasiOpts =
                WasiOptions.builder()
                        .withStdout(stdout)
                        .withStderr(stderr)
                        .withStdin(stdin)
                        .build();

        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var quickjs =
                Instance.builder(loadModule("compiled/quickjs-provider.javy-dynamic.wasm"))
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var greetingMsg = "Hello QuickJS!";

        byte[] jsCode = ("console.log(\"" + greetingMsg + "\");").getBytes(UTF_8);
        var ptr =
                quickjs.export("canonical_abi_realloc")
                        .apply(
                                0, // original_ptr
                                0, // original_size
                                1, // alignment
                                jsCode.length // new size
                                )[0];

        quickjs.memory().write((int) ptr, jsCode);
        var aggregatedCodePtr = quickjs.export("compile_src").apply(ptr, jsCode.length)[0];

        var codePtr = quickjs.memory().readI32((int) aggregatedCodePtr); // 32 bit
        var codeLength = quickjs.memory().readU32((int) aggregatedCodePtr + 4);

        quickjs.export("eval_bytecode").apply(codePtr, codeLength);

        // stderr?
        assertEquals(greetingMsg + "\n", stderr.toString(UTF_8));
    }

    @Test
    public void shouldUseDynamicallyLinkedJavyModules() {
        ByteArrayInputStream stdin = new ByteArrayInputStream("".getBytes(UTF_8));
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();

        var wasiOpts =
                WasiOptions.builder()
                        .withStdout(stdout)
                        .withStderr(stderr)
                        .withStdin(stdin)
                        .build();

        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var quickjs =
                Instance.builder(loadModule("compiled/quickjs-provider.javy-dynamic.wasm"))
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var store = new Store();
        store.register("javy_quickjs_provider_v1", quickjs);

        Instance.builder(loadModule("compiled/hello-world.js.javy-dynamic.wasm"))
                .withImportValues(store.toImportValues())
                .build();

        // stderr?
        assertEquals("Hello world dynamic Javy!\n", stderr.toString(UTF_8));
    }

    @Test
    public void shouldRunTinyGoModule() {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = WasiPreview1.builder().withLogger(this.logger).withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        var module = loadModule("compiled/sum.go.tiny.wasm");
        var instance = Instance.builder(module).withImportValues(imports).build();
        var sum = instance.export("add");
        var result = sum.apply(20, 22)[0];

        assertEquals(result, 42);
    }

    @Test
    public void shouldRunWasiGoModule() {
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).build();
        var wasi = WasiPreview1.builder().withLogger(this.logger).withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
        var module = loadModule("compiled/main.go.wasm");
        var exit =
                assertThrows(
                        WasiExitException.class,
                        () -> Instance.builder(module).withImportValues(imports).build());
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
        var wasi = WasiPreview1.builder().withLogger(this.logger).withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

        var module = loadModule("compiled/basic.dotnet.wasm");
        Instance.builder(module).withImportValues(imports).build();

        assertEquals("Hello, Wasi Console!\n", fakeStdout.output());
    }

    @Test
    public void wasiRandom() {
        var seed = 0x12345678;
        var wasi =
                WasiPreview1.builder()
                        .withLogger(this.logger)
                        .withOptions(WasiOptions.builder().withRandom(new Random(seed)).build())
                        .build();

        var memory = new ByteBufferMemory(new MemoryLimits(8, 8));
        assertEquals(0, wasi.randomGet(memory, 0, 123_456));
        assertEquals(0, wasi.randomGet(memory, 222_222, 87_654));

        var random = new Random(seed);
        byte[] first = new byte[123_456];
        random.nextBytes(first);
        byte[] second = new byte[87_654];
        random.nextBytes(second);

        assertArrayEquals(first, memory.readBytes(0, 123_456));
        assertArrayEquals(second, memory.readBytes(222_222, 87_654));
    }

    @Test
    public void wasiPositionedWriteWithAppendShouldFail() throws IOException {
        try (var fs = newZeroFs()) {
            var dir = "fs-tests.dir";
            Path source = new File("../wasi-testsuite/tests/c/testsuite").toPath().resolve(dir);
            Path target = fs.getPath(dir);
            copyDirectory(source, target);

            try (var wasi = wasiWithDirectory(target.toString(), target)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));

                int fdPtr = 0;
                int result =
                        wasi.pathOpen(
                                memory,
                                3,
                                WasiLookupFlags.SYMLINK_FOLLOW,
                                "pwrite.cleanup",
                                WasiOpenFlags.CREAT,
                                0,
                                0,
                                WasiFdFlags.APPEND,
                                fdPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);

                int fd = memory.readInt(fdPtr);
                assertEquals(4, fd);

                result = wasi.fdPwrite(memory, fd, 0, 0, 0, 0);
                assertEquals(WasiErrno.ENOTSUP.value(), result);
            }
        }
    }

    @Test
    public void wasiReadLink() throws IOException {
        try (var fs = newZeroFs()) {
            Path root = fs.getPath("test");
            createDirectory(root);
            createSymbolicLink(root.resolve("abc"), fs.getPath("xyz"));
            try (var wasi = wasiWithDirectory(root.toString(), root)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));
                int bufPtr = 0;
                int bufUsedPtr = 16;

                int result = wasi.pathReadlink(memory, 3, "abc", bufPtr, 16, bufUsedPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);

                int length = memory.readInt(bufUsedPtr);
                assertEquals(3, length);
                String name = memory.readString(bufPtr, length);
                assertEquals("xyz", name);
            }
        }
    }

    @Test
    public void wasiPollOneoffNoSubscriptions() {
        var wasi = WasiPreview1.builder().build();
        var memory = new ByteBufferMemory(new MemoryLimits(0));
        int result = wasi.pollOneoff(memory, 0, 0, 0, 0);
        assertEquals(WasiErrno.EINVAL.value(), result);
    }

    @Test
    public void wasiPollOneoffStdinNoData() {
        var stdin = new ByteArrayInputStream("".getBytes(UTF_8));
        var wasiOpts = WasiOptions.builder().withStdin(stdin).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 2;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.CLOCK);
        memory.writeI32(in + 16, WasiClockId.REALTIME);
        memory.writeLong(in + 16 + 8, MILLISECONDS.toNanos(200));
        memory.writeLong(in + 16 + 16, 0); // precision
        memory.writeShort(in + 16 + 24, (short) 0);
        in += 48;
        memory.writeLong(in, 0xAAAA_BBBB_CCCC_DDDDL); // userdata
        memory.writeByte(in + 8, WasiEventType.FD_READ);
        memory.writeI32(in + 16, 0); // fd
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.CLOCK, memory.read(outPtr + 10));
    }

    @Test
    public void wasPollOneoffStdinWithData() {
        var stdin = new ByteArrayInputStream("Hello, World!".getBytes(UTF_8));
        var wasiOpts = WasiOptions.builder().withStdin(stdin).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 1;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.FD_READ);
        memory.writeI32(in + 16, 0); // fd
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.FD_READ, memory.read(outPtr + 10));
    }

    @Test
    public void wasiPollOneoffRegularFile() {
        try (var fs = newZeroFs()) {
            Path root = fs.getPath("test");
            createDirectory(root);
            var file = root.resolve("hello.txt");
            writeString(file, "Hello, World!");
            try (var wasi = wasiWithDirectory(root.toString(), root)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));
                int fdPtr = 1024;
                int result = wasi.pathOpen(memory, 3, 0, "hello.txt", 0, 0, 0, 0, fdPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);
                int fd = memory.readInt(fdPtr);
                assertEquals(4, fd);

                int nsubscriptions = 2;
                int neventsPtr = 0;
                int inPtr = 4;
                int outPtr = inPtr + nsubscriptions * 48;
                int in = inPtr;
                memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
                memory.writeByte(in + 8, WasiEventType.FD_READ);
                memory.writeI32(in + 16, fd);
                in += 48;
                memory.writeLong(in, 0xAAAA_BBBB_CCCC_DDDDL); // userdata
                memory.writeByte(in + 8, WasiEventType.FD_WRITE);
                memory.writeI32(in + 16, fd);
                result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);
                assertEquals(2, memory.readInt(neventsPtr));
                int out = outPtr;
                assertEquals(0x8888_7777_6666_5555L, memory.readLong(out));
                assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(out + 8));
                assertEquals(WasiEventType.FD_READ, memory.read(out + 10));
                out += 32;
                assertEquals(0xAAAA_BBBB_CCCC_DDDDL, memory.readLong(out));
                assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(out + 8));
                assertEquals(WasiEventType.FD_WRITE, memory.read(out + 10));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void wasiPollOneoffClockAbstime() {
        long deadline = System.nanoTime() + MILLISECONDS.toNanos(25);
        var wasi = WasiPreview1.builder().build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 1;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.CLOCK);
        memory.writeI32(in + 16, WasiClockId.MONOTONIC);
        memory.writeLong(in + 16 + 8, deadline);
        memory.writeLong(in + 16 + 16, 0); // precision
        memory.writeShort(in + 16 + 24, (short) WasiSubClockFlags.SUBSCRIPTION_CLOCK_ABSTIME);
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.CLOCK, memory.read(outPtr + 10));
        assertTrue(System.nanoTime() >= deadline);
    }

    private static FileSystem newZeroFs() {
        return ZeroFs.newFileSystem(
                Configuration.unix().toBuilder().setAttributeViews("unix").build());
    }

    private static WasiPreview1 wasiWithDirectory(String guest, Path host) {
        var options = WasiOptions.builder().withDirectory(guest, host).build();
        return WasiPreview1.builder().withOptions(options).build();
    }

    // TODO: clean this up
    // starts, ends but does nothing, need to dig down!
    // TODO: note to self:
    // try to run this example first:
    // https://github.com/WebAssembly/threads/blob/b2567bff61ee6fbe731934f0ed17a5d48dc9ab01/proposals/threads/Overview.md#example
    @Test
    @Timeout(value = 30, unit = MINUTES)
    @Disabled
    public void runProtoc() throws Exception {
        var store = new Store();

        var memModule =
                Parser.parse(
                        new File(
                                "/home/andreatp/workspace/go-protoc-gen-grpc-java/internal/wasm/memory.wasm"));
        var memInstance = Instance.builder(memModule).build();
        store.register("env", memInstance);
        try (var fs = newZeroFs()) {
            var dir = "protos";
            Path source = new File("./src/test/resources/protoc-test").toPath().resolve(dir);
            Path target = fs.getPath("/");
            copyDirectory(source, target);

            try (var wasi =
                    WasiPreview1.builder()
                            .withOptions(
                                    WasiOptions.builder()
                                            .inheritSystem()
                                            .withStdin(new ByteArrayInputStream("".getBytes()))
                                            .withArguments(
                                                    List.of(
                                                            "protoc-gen-java",
                                                            "-grpc-java_out=out/grpc-java",
                                                            "-Iprotos helloworld.proto"))
                                            .withDirectory(target.toString(), target)
                                            .build())
                            .build()) {
                var imports =
                        ImportValues.builder()
                                .addFunction(wasi.toHostFunctions())
                                .addMemory(
                                        new ImportMemory(
                                                "env",
                                                "memory",
                                                new ByteArrayMemory(
                                                        new MemoryLimits(
                                                                /* 7 */ 4,
                                                                MemoryLimits.MAX_PAGES,
                                                                true))))
                                .build();
                var module =
                        Parser.parse(
                                new File(
                                        "/home/andreatp/workspace/go-protoc-gen-grpc-java/internal/wasm/protoc-gen-grpc-java.wasm"));

                store.addImportValues(imports);
                store.instantiate("gen-grpc", module);
                //                var instance =
                //
                // Instance.builder(module).withImportValues(imports).withStart(false).build();
                //                instance.exports().function("_start").apply();
            }
        }
    }
}
