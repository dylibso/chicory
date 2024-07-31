package com.dylibso.chicory.approvals;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Module;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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
        verifyGeneratedBytecode("hello-wasi.wat.wasm");
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
        verifyGeneratedBytecode("start.wat.wasm");
    }

    @Test
    public void verifyTrap() {
        verifyGeneratedBytecode("trap.wat.wasm");
    }

    private static void verifyGeneratedBytecode(String name) {
        var instance =
                Instance.builder(
                                Module.builder(
                                                ClassLoader.getSystemClassLoader()
                                                        .getResourceAsStream("compiled/" + name))
                                        .build())
                        .withImportValidation(false)
                        .withMachineFactory(AotMachine::new)
                        .withStart(false)
                        .build();
        var compiled = ((AotMachine) instance.getMachine()).compiledClass();

        ClassReader cr = new ClassReader(compiled);
        var out = new ByteArrayOutputStream();
        cr.accept(new TraceClassVisitor(new PrintWriter(out)), 0);

        Approvals.verify(out);
    }
}
