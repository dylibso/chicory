package wasi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public class WasiPreview1Test {

    private static WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    @Test
    public void shouldRunWasiModule() {
        // check with: wasmtime src/test/resources/compiled/hello-wasi.wat.wasm
        var fakeStdout = new MockPrintStream();
        var wasi =
                WasiPreview1.builder()
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
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
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
        var fakeStdin = new ByteArrayInputStream("{ \"n\": 2, \"bar\": \"baz\"}".getBytes(UTF_8));
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).withStdin(fakeStdin).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
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
        assertEquals(greetingMsg + "\n", new String(stderr.toByteArray(), UTF_8));
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
        assertEquals("Hello world dynamic Javy!\n", new String(stderr.toByteArray(), UTF_8));
    }

    @Test
    public void shouldRunTinyGoModule() {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
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
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
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
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
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
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

        var module = loadModule("compiled/basic.dotnet.wasm");
        Instance.builder(module).withImportValues(imports).build();

        assertEquals("Hello, Wasi Console!\n", fakeStdout.output());
    }

    @Test
    public void shouldRunWasiSwiftModule() throws Exception {
        var fakeStdout = new MockPrintStream();
        var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var imports = ImportValues.builder().addFunction(wasi.toHostFunctions()).build();

        var module = loadModule("compiled/hello-world.swift.wasm");
        Instance.builder(module).withImportValues(imports).build();

        assertEquals("Hello, Swift world!\n", fakeStdout.output());
    }

    @Test
    public void shouldRunWasiSwiftModuleWithImportExport() throws Exception {
        var wasiOpts = WasiOptions.builder().build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var imports =
                ImportValues.builder()
                        .addFunction(wasi.toHostFunctions())
                        .addFunction(
                                new HostFunction(
                                        "env",
                                        "operation",
                                        FunctionType.of(
                                                List.of(ValType.I32, ValType.I32),
                                                List.of(ValType.I32)),
                                        (inst, args) -> {
                                            var x = args[0];
                                            var y = args[1];

                                            return new long[] {x * y};
                                        }))
                        .build();

        var module = loadModule("compiled/calculator.swift.wasm");
        var instance = Instance.builder(module).withImportValues(imports).withStart(false).build();

        var result = (int) instance.exports().function("run").apply(2, 3)[0];

        assertEquals(6, result);
    }
}
