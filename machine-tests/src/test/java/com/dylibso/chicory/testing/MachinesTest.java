package com.dylibso.chicory.testing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.runtime.ImportTable;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.TrapException;
import com.dylibso.chicory.testing.gen.DynamicHelloJSModule;
import com.dylibso.chicory.testing.gen.QuickJSModule;
import com.dylibso.chicory.wabt.Wat2WasmModule;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.ValType;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
        return Parser.parse(getClass().getResourceAsStream("/" + fileName));
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

    // quickjs -> pre-compiled aot
    // module -> interpreter / runtime aot
    @Test
    public void shouldRunQuickJsPrecompiled() {
        var stderr = new ByteArrayOutputStream();

        var wasi = setupWasi(stderr);
        // using the pre-compiled version of QuickJS
        var quickjs =
                quickJsInstanceBuilder()
                        .withMachineFactory(QuickJSModule::create)
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
                .withMachineFactory(AotMachine::new)
                .withImportValues(store.toImportValues())
                .build();

        assertEquals(expectedOutput + expectedOutput, stderr.toString(UTF_8));
    }

    // quickjs -> interpreter
    // module -> pre-compiled aot / runtime aot
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

        // the module is going to use the pre compiled aot
        moduleInstanceBuilder()
                .withMachineFactory(DynamicHelloJSModule::create)
                .withImportValues(store.toImportValues())
                .build();

        // stderr?
        assertEquals(expectedOutput, stderr.toString(UTF_8));

        // and now runtime AOT
        moduleInstanceBuilder()
                .withMachineFactory(AotMachine::new)
                .withImportValues(store.toImportValues())
                .build();

        assertEquals(expectedOutput + expectedOutput, stderr.toString(UTF_8));
    }

    // quickjs -> runtime aot
    // module -> pre-compiled aot / interpreter
    @Test
    public void shouldRunQuickJsRuntimeAot() {
        var stderr = new ByteArrayOutputStream();

        var wasi = setupWasi(stderr);
        // using the runtime aot version of QuickJS
        var quickjs =
                quickJsInstanceBuilder()
                        .withMachineFactory(AotMachine::new)
                        .withImportValues(
                                ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
                        .build();

        var store = new Store().register("javy_quickjs_provider_v1", quickjs);

        // the module is going to use the pre compiled aot
        moduleInstanceBuilder()
                .withMachineFactory(DynamicHelloJSModule::create)
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
                Jimfs.newFileSystem(
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
                                    var machine = Wat2WasmModule.create(inst);
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

    @Test
    public void shouldCallIndirectInterpreterToAot() {
        var store = new Store();
        var table = new TableInstance(new Table(ValType.FuncRef, new TableLimits(3, 3)));
        store.addTable(new ImportTable("test", "table", table));

        var instance =
                Instance.builder(loadModule("compiled/call_indirect-export.wat.wasm"))
                        .withImportValues(store.toImportValues())
                        .withMachineFactory(InterpreterMachine::new)
                        .build();
        store.register("test", instance);

        Instance.builder(loadModule("compiled/call_indirect-import.wat.wasm"))
                .withImportValues(store.toImportValues())
                .withMachineFactory(AotMachine::new)
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
        var table = new TableInstance(new Table(ValType.FuncRef, new TableLimits(3, 3)));
        store.addTable(new ImportTable("test", "table", table));

        var instance =
                Instance.builder(loadModule("compiled/call_indirect-export.wat.wasm"))
                        .withImportValues(store.toImportValues())
                        .withMachineFactory(AotMachine::new)
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
