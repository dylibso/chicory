package com.dylibso.chicory.experimental.build.time.aot;

import static com.dylibso.chicory.corpus.WatGenerator.methodTooLarge;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.experimental.aot.InterpreterFallback;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InterpreterFallbackTest {

    static Path classDir;
    static Path wasmFile;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path srcDir = Path.of("target", "test-fixtures", "src");
        Files.createDirectories(srcDir);
        classDir = Path.of("target", "test-fixtures", "classes");
        Files.createDirectories(classDir);

        String wat = methodTooLarge(20_000);
        var wasm = Wat2Wasm.parse(wat);

        wasmFile = srcDir.resolve("main.wasm");
        Files.write(wasmFile, wasm);
    }

    private Config.Builder defaultConfig() {
        return Config.builder()
                .withWasmFile(wasmFile)
                .withTargetSourceFolder(classDir)
                .withTargetClassFolder(classDir)
                .withTargetWasmFolder(classDir);
    }

    private void generateAll(Generator generator) throws IOException {
        generator.generateSources();
        generator.generateResources();
        generator.generateMetaWasm();
    }

    @Test
    public void testDefaultInterpreterFallback() throws IOException {
        var config =
                defaultConfig()
                        .withName("com.dylibso.chicory.experimental.build.time.aot.Test1")
                        // .withInterpreterFallback(InterpreterFallback.FAIL)
                        .build();
        var generator = new Generator(config);

        var exception = assertThrows(ChicoryException.class, () -> generateAll(generator));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "WASM function size"
                                        + " exceeds the Java method size limits and cannot be"
                                        + " compiled to Java bytecode. It can only be run in the"
                                        + " interpreter. Either reduce the size of the function or"
                                        + " enable the interpreter fallback mode: WASM function"
                                        + " index: 2"),
                exception.getMessage());
    }

    @Test
    public void testWarnInterpreterFallback() throws IOException {
        var config =
                defaultConfig()
                        .withName("com.dylibso.chicory.experimental.build.time.aot.Test2")
                        .withInterpreterFallback(InterpreterFallback.WARN)
                        .build();
        var generator = new Generator(config);

        var orignalErr = System.err;
        try {
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(stdErr, true, UTF_8));

            generateAll(generator);

            assertTrue(
                    stdErr.toString(UTF_8)
                            .startsWith(
                                    "Warning: using interpreted mode for WASM function index: 2"));
        } finally {
            System.setErr(orignalErr);
        }
    }

    @Test
    public void testSilentInterpreterFallback() throws IOException, ClassNotFoundException {
        var config =
                defaultConfig()
                        .withName("com.dylibso.chicory.experimental.build.time.aot.Test3")
                        .withInterpreterFallback(InterpreterFallback.SILENT)
                        .build();
        var generator = new Generator(config);

        var orignalStdErr = System.err;
        var orignalStdOut = System.out;
        try {
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(stdErr, true, UTF_8));

            ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(stdOut, true, UTF_8));

            generateAll(generator);

            assertEquals("", stdErr.toString(UTF_8));
            assertEquals("", stdOut.toString(UTF_8));
        } finally {
            System.setErr(orignalStdErr);
            System.setOut(orignalStdOut);
        }

        // Let's load what was just generated.  This lets us check that the generated module
        // metadata
        // works for AOT cases where the interpreter is used.

        var url = classDir.toUri().toURL();
        var cl = new URLClassLoader(new URL[] {url});
        var machineClass =
                cl.loadClass("com.dylibso.chicory.experimental.build.time.aot.Test3Machine");
        Function<Instance, Machine> machineFactory = createMachineFactory(machineClass);

        var hostStackTrace = new ArrayList<String>();
        var hostFunc =
                new HostFunction(
                        "funcs",
                        "host_func",
                        FunctionType.of(List.of(ValType.I32), List.of(ValType.I32)),
                        (inst, args) -> {
                            var thread = Thread.currentThread();
                            int i = 0;
                            for (StackTraceElement element : thread.getStackTrace()) {
                                i++;
                                if (i < 2 || i > 21) {
                                    continue;
                                }
                                hostStackTrace.add(
                                        element.getClassName() + "." + element.getMethodName());
                            }
                            return new long[] {35};
                        });

        WasmModule module;
        try (InputStream in = machineClass.getResourceAsStream("Test3.meta")) {
            module = Parser.parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load AOT WASM module", e);
        }
        var instance =
                Instance.builder(module)
                        .withImportValues(ImportValues.builder().addFunction(hostFunc).build())
                        .withMachineFactory(machineFactory)
                        .withStart(false)
                        .build();

        assertEquals(35, instance.export("func_2").apply(0)[0]);

        var stackTrace = String.join("\n", hostStackTrace);
        Approvals.verify(stackTrace);

        hostStackTrace.clear();
        assertEquals(35, instance.export("func_2").apply(1)[0]);

        stackTrace = String.join("\n", hostStackTrace);
        Approvals.verify(
                stackTrace,
                new Options()
                        .forFile()
                        .withBaseName(
                                "InterpreterFallbackTest.testSilentInterpreterFallback-indirect"));
    }

    private Function<Instance, Machine> createMachineFactory(Class<?> machineClass) {
        try {
            var clazz = machineClass.asSubclass(Machine.class);
            // convert constructor to factory interface
            var constructor = clazz.getConstructor(Instance.class);
            var handle = publicLookup().unreflectConstructor(constructor);
            @SuppressWarnings("unchecked")
            Function<Instance, Machine> function = asInterfaceInstance(Function.class, handle);
            return function;
        } catch (ReflectiveOperationException e) {
            throw new ChicoryException(e);
        }
    }
}
