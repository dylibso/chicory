package com.dylibso.chicory.experimental.aot.cli;

import static com.dylibso.chicory.corpus.WatGenerator.methodTooLarge;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

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

    private String[] args(String... args) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(List.of(args));
        result.addAll(
                List.of(
                        "--source-dir=" + classDir,
                        "--wasm-dir=" + classDir,
                        "--class-dir=" + classDir,
                        wasmFile.toString()));
        return result.toArray(new String[result.size()]);
    }

    @Test
    public void testDefaultInterpreterFallback() throws IOException {

        var cli = new Cli();
        CommandLine cmd = new CommandLine(cli);

        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        cmd.setErr(new PrintWriter(stdErr, true, UTF_8));
        var exitCode = cmd.execute(args("--prefix=com.dylibso.chicory.experimental.aot.cli.Test1"));

        assertEquals(1, exitCode);
        assertTrue(
                stdErr.toString(UTF_8)
                        .startsWith(
                                "com.dylibso.chicory.wasm.ChicoryException: Interpreter needed (but"
                                        + " disabled) for WASM function index: 2"));
    }

    @Test
    public void testWarnInterpreterFallback() throws IOException {
        var cli = new Cli();
        CommandLine cmd = new CommandLine(cli);

        var orignalErr = System.err;
        try {
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(stdErr, true, UTF_8));
            cmd.setErr(new PrintWriter(stdErr, true, UTF_8));

            var exitCode =
                    cmd.execute(
                            args(
                                    "--interpreter-fallback=WARN",
                                    "--prefix=com.dylibso.chicory.experimental.aot.cli.Test2"));

            assertEquals(0, exitCode);
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
        var cli = new Cli();
        CommandLine cmd = new CommandLine(cli);

        var orignalStdErr = System.err;
        var orignalStdOut = System.out;
        try {
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(stdErr, true, UTF_8));
            cmd.setErr(new PrintWriter(stdErr, true, UTF_8));

            ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(stdOut, true, UTF_8));
            cmd.setOut(new PrintWriter(stdOut, true, UTF_8));

            var exitCode =
                    cmd.execute(
                            args(
                                    "--interpreter-fallback=SILENT",
                                    "--prefix=com.dylibso.chicory.experimental.aot.cli.Test3"));

            assertEquals(0, exitCode);
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
        var machineClass = cl.loadClass("com.dylibso.chicory.experimental.aot.cli.Test3Machine");
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

        assertEquals(
                List.of(
                        "com.dylibso.chicory.experimental.aot.cli.InterpreterFallbackTest.lambda$testSilentInterpreterFallback$0",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine$AotMethods.callHostFunction",
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.func_0",
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.func_1",
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.call_1",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine$MachineCall.call",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine.call",
                        // here is where the Interpreter switches back to AOT for the call to func_1
                        "com.dylibso.chicory.runtime.AotInterpreterMachine.CALL",
                        "com.dylibso.chicory.runtime.InterpreterMachine.eval",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine.call",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine$AotMethods.callIndirect",
                        // here is where the AOT method switches to the interpreter
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.func_2",
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.func_3",
                        "com.dylibso.chicory.experimental.aot.cli.Test3MachineFuncGroup_0.call_3",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine$MachineCall.call",
                        "com.dylibso.chicory.experimental.aot.cli.Test3Machine.call",
                        "com.dylibso.chicory.runtime.Instance$Exports.lambda$function$0",
                        "com.dylibso.chicory.experimental.aot.cli.InterpreterFallbackTest.testSilentInterpreterFallback"),
                hostStackTrace);
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
