package com.dylibso.chicory.testing;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportTable;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.testing.gen.DynamicHelloJS;
import com.dylibso.chicory.testing.gen.QuickJS;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.ValType;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public final class MachinesTest {

    private WasmModule loadModule(String fileName) {
        return Parser.parse(CorpusResources.getResource(fileName));
    }

    private Instance.Builder quickJsInstanceBuilder() {
        return Instance.builder(loadModule("compiled/quickjs-provider.javy-dynamic.wasm"));
    }

    private Instance.Builder moduleInstanceBuilder() {
        return Instance.builder(loadModule("compiled/hello-world.js.javy-dynamic.wasm"));
    }

    private WasiPreview1 setupWasi(ByteArrayOutputStream stderr) {
        InputStream stdin = InputStream.nullInputStream();
        var stdout = new ByteArrayOutputStream();

        var wasiOpts =
                WasiOptions.builder()
                        .withStdout(stdout)
                        .withStderr(stderr)
                        .withStdin(stdin)
                        .build();

        return WasiPreview1.builder().withOptions(wasiOpts).build();
    }

    private static final String expectedOutput = "Hello world dynamic Javy!\n";

    // quickjs -> build time compiled
    // module -> interpreter / runtime compiled
    @Test
    public void shouldRunQuickJsPrecompiled() {
        var stderr = new ByteArrayOutputStream();

        var wasi = setupWasi(stderr);
        // using the pre-compiled version of QuickJS
        var quickjs =
                quickJsInstanceBuilder()
                        .withMachineFactory(QuickJS::create)
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var store = new Store().register("javy_quickjs_provider_v1", quickjs);

        // the module is going to use the interpreter instead
        moduleInstanceBuilder().withImportValues(store.toImportValues()).build();

        // stderr?
        assertEquals(expectedOutput, stderr.toString(UTF_8));

        // and now runtime AOT
        moduleInstanceBuilder()
                .withMachineFactory(MachineFactoryCompiler::compile)
                .withImportValues(store.toImportValues())
                .build();

        assertEquals(expectedOutput + expectedOutput, stderr.toString(UTF_8));
    }

    // quickjs -> interpreter
    // module -> build time compiler / runtime compiler
    @Test
    public void shouldRunQuickJsInterpreted() {
        var stderr = new ByteArrayOutputStream();

        var wasi = setupWasi(stderr);
        // using the pre-compiled version of QuickJS
        var quickjs =
                quickJsInstanceBuilder()
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var store = new Store().register("javy_quickjs_provider_v1", quickjs);

        // the module is going to use the build time compiled
        moduleInstanceBuilder()
                .withMachineFactory(DynamicHelloJS::create)
                .withImportValues(store.toImportValues())
                .build();

        // stderr?
        assertEquals(expectedOutput, stderr.toString(UTF_8));

        // and now runtime compiler
        moduleInstanceBuilder()
                .withMachineFactory(MachineFactoryCompiler::compile)
                .withImportValues(store.toImportValues())
                .build();

        assertEquals(expectedOutput + expectedOutput, stderr.toString(UTF_8));
    }

    // quickjs -> runtime compiler
    // module -> build time compiler / interpreter
    @Test
    public void shouldRunQuickJsRuntimeCompiled() {
        var stderr = new ByteArrayOutputStream();

        var wasi = setupWasi(stderr);
        // using the runtime compiled version of QuickJS
        var quickjs =
                quickJsInstanceBuilder()
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var store = new Store().register("javy_quickjs_provider_v1", quickjs);

        // the module is going to use the build time compiled
        moduleInstanceBuilder()
                .withMachineFactory(DynamicHelloJS::create)
                .withImportValues(store.toImportValues())
                .build();

        // stderr?
        assertEquals(expectedOutput, stderr.toString(UTF_8));

        // and now the interpreter
        moduleInstanceBuilder().withImportValues(store.toImportValues()).build();

        assertEquals(expectedOutput + expectedOutput, stderr.toString(UTF_8));
    }

    @Test
    public void shouldUseMachineCallOnlyForExport() throws Exception {
        ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();

        var watFile = new File("../wasm-corpus/src/main/resources/wat/iterfact.wat");

        try (FileSystem fs =
                ZeroFs.newFileSystem(
                        Configuration.unix().toBuilder().setAttributeViews("unix").build())) {

            Path target = fs.getPath("tmp");
            java.nio.file.Files.createDirectory(target);
            Path path = target.resolve(watFile.getName());
            copy(new FileInputStream(watFile), path, StandardCopyOption.REPLACE_EXISTING);

            WasiOptions wasiOpts =
                    WasiOptions.builder()
                            .withStdout(stdoutStream)
                            .withStderr(stdoutStream)
                            .withDirectory(target.toString(), target)
                            .withArguments(List.of("wat2wasm", path.toString(), "--output=-"))
                            .build();
            try (var wasi = WasiPreview1.builder().withOptions(wasiOpts).build()) {
                ImportValues imports =
                        ImportValues.builder().addFunction(wasi.toHostFunctions()).build();
                var wat2WasmModule = Parser.parse(new File("../wabt/src/main/resources/wat2wasm"));
                var startFunctionIndex = new AtomicInteger();
                for (int i = 0; i < wat2WasmModule.exportSection().exportCount(); i++) {
                    var export = wat2WasmModule.exportSection().getExport(i);
                    if (export.name().equals("_start")
                            && export.exportType() == ExternalType.FUNCTION) {
                        startFunctionIndex.set(export.index());
                    }
                }
                // This index is subject to change when we update the wat2wasm version
                assertEquals(18, startFunctionIndex.get());

                Instance.builder(Parser.parse(new File("../wabt/src/main/resources/wat2wasm")))
                        .withMachineFactory(
                                (inst) -> {
                                    var machine = Wat2Wasm.create(inst);
                                    return (funcId, args) -> {
                                        assertEquals(startFunctionIndex.get(), funcId);
                                        return machine.call(funcId, args);
                                    };
                                })
                        .withImportValues(imports)
                        .build();
            }

            var result = stdoutStream.toByteArray();

            assertTrue(result.length > 0);
            assertTrue(new String(result, UTF_8).contains("iterFact"));
        }
    }

    private Instance buildKotlinWasm(
            ByteArrayOutputStream stdout, Instance.Builder instanceBuilder) {
        var wasi =
                WasiPreview1.builder()
                        .withOptions(WasiOptions.builder().withStdout(stdout).build())
                        .build();

        var instance =
                instanceBuilder
                        .withStart(false)
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        instance.export("_initialize").apply();
        return instance;
    }

    private void assertKotlinWasiOutput(String output) {
        assertTrue(output.contains("Hello from Kotlin via WASI"), output);
        assertTrue(output.contains("Current 'realtime' timestamp is:"), output);
        assertTrue(output.contains("Current 'monotonic' timestamp is:"), output);

        int realtimeIdx = output.indexOf("Current 'realtime' timestamp is: ");
        var realtimeLine = output.substring(realtimeIdx).lines().findFirst().orElseThrow();
        var realtimeValue =
                Long.parseLong(realtimeLine.substring(realtimeLine.lastIndexOf(' ') + 1).trim());
        assertTrue(realtimeValue > 0, "Expected positive realtime, got: " + realtimeValue);

        int monotonicIdx = output.indexOf("Current 'monotonic' timestamp is: ");
        var monotonicLine = output.substring(monotonicIdx).lines().findFirst().orElseThrow();
        var monotonicValue =
                Long.parseLong(monotonicLine.substring(monotonicLine.lastIndexOf(' ') + 1).trim());
        assertTrue(monotonicValue > 0, "Expected positive monotonic, got: " + monotonicValue);
    }

    @Test
    public void shouldRunKotlinWasmInterpreted() {
        var stdout = new ByteArrayOutputStream();
        var module = loadModule("compiled/hello-world.kt.wasm");
        buildKotlinWasm(stdout, Instance.builder(module));

        assertKotlinWasiOutput(stdout.toString(UTF_8));
    }

    @Test
    public void shouldRunKotlinWasmCompiled() {
        var stdout = new ByteArrayOutputStream();
        var module = loadModule("compiled/hello-world.kt.wasm");
        buildKotlinWasm(
                stdout,
                Instance.builder(module).withMachineFactory(MachineFactoryCompiler::compile));

        assertKotlinWasiOutput(stdout.toString(UTF_8));
    }

    @Test
    public void shouldCallIndirectInterpreterToAot() {
        var store = new Store();
        var table =
                new TableInstance(
                        new Table(ValType.FuncRef, new TableLimits(3, 3)), REF_NULL_VALUE);
        store.addTable(new ImportTable("test", "table", table));

        var instance =
                Instance.builder(loadModule("compiled/call_indirect-export.wat.wasm"))
                        .withImportValues(store.toImportValues())
                        .withMachineFactory(InterpreterMachine::new)
                        .build();
        store.register("test", instance);

        Instance.builder(loadModule("compiled/call_indirect-import.wat.wasm"))
                .withImportValues(store.toImportValues())
                .withMachineFactory(MachineFactoryCompiler::compile)
                .build();

        assertEquals(42, instance.export("call-self").apply()[0]);
        assertEquals(88, instance.export("call-other").apply()[0]);

        var ex = assertThrows(TrapException.class, instance.export("call-other-fail")::apply);
        var className = ex.getStackTrace()[0].getClassName();
        assertTrue(className.contains("CompiledMachine"), className);
    }

    @Test
    public void shouldCallIndirectAotToInterpreter() {
        var store = new Store();
        var table =
                new TableInstance(
                        new Table(ValType.FuncRef, new TableLimits(3, 3)), REF_NULL_VALUE);
        store.addTable(new ImportTable("test", "table", table));

        var instance =
                Instance.builder(loadModule("compiled/call_indirect-export.wat.wasm"))
                        .withImportValues(store.toImportValues())
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();
        store.register("test", instance);

        Instance.builder(loadModule("compiled/call_indirect-import.wat.wasm"))
                .withImportValues(store.toImportValues())
                .withMachineFactory(InterpreterMachine::new)
                .build();

        assertEquals(42, instance.export("call-self").apply()[0]);
        assertEquals(88, instance.export("call-other").apply()[0]);

        var ex = assertThrows(TrapException.class, instance.export("call-other-fail")::apply);
        var className = ex.getStackTrace()[0].getClassName();
        assertTrue(className.contains("InterpreterMachine"), className);
    }
}
