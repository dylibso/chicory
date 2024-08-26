package com.dylibso.chicory.approvals;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class ApprovalTest {

    @Test
    public void verifyBranching() {
        verifyGeneratedBytecode("branching.wat.wasm");
    }

    @Test
    public void verifyBrTable() {
        verifyGeneratedBytecode("br_table.wat.wasm");
    }

    @Test
    public void verifyFloat() {
        verifyGeneratedBytecode("float.wat.wasm");
    }

    @Test
    public void verifyHelloWasi() {
        verifyGeneratedBytecode(
                "hello-wasi.wat.wasm",
                new HostFunction(
                        (instance, args) -> null,
                        "wasi_snapshot_preview1",
                        "fd_write",
                        List.of(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                        List.of(ValueType.I32)));
    }

    @Test
    public void verifyI32() {
        verifyGeneratedBytecode("i32.wat.wasm");
    }

    @Test
    public void verifyIterFact() {
        verifyGeneratedBytecode("iterfact.wat.wasm");
    }

    @Test
    public void verifyKitchenSink() {
        verifyGeneratedBytecode("kitchensink.wat.wasm");
    }

    @Test
    public void verifyMemory() {
        verifyGeneratedBytecode("memory.wat.wasm");
    }

    @Test
    public void verifyStart() {
        verifyGeneratedBytecode(
                "start.wat.wasm",
                new HostFunction(
                        (instance, args) -> null,
                        "env",
                        "gotit",
                        List.of(ValueType.I32),
                        List.of()));
    }

    @Test
    public void verifyTrap() {
        verifyGeneratedBytecode("trap.wat.wasm");
    }

    private static void verifyGeneratedBytecode(String name, HostFunction... hostFunctions) {
        var instance =
                Instance.builder(
                                Parser.parse(
                                        ClassLoader.getSystemClassLoader()
                                                .getResourceAsStream("compiled/" + name)))
                        .withHostImports(new HostImports(hostFunctions))
                        .withMachineFactory(AotMachine::new)
                        .withStart(false)
                        .build();
        var compiled = ((AotMachine) instance.getMachine()).compiledClass();

        ClassReader cr = new ClassReader(compiled);
        var out = new ByteArrayOutputStream();
        cr.accept(new TraceClassVisitor(new PrintWriter(out, false, UTF_8)), 0);

        Approvals.verify(out);
    }
}
