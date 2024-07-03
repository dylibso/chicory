package com.dylibso.chicory.approvals;

import com.dylibso.chicory.aot.AotMachine;
import com.dylibso.chicory.runtime.Module;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class ApprovalTest {

    @Test
    public void verifyGeneratedBytecode() throws IOException {
        var instance = Module.builder("compiled/iterfact.wat.wasm").build().instantiate();
        var compiled = new AotMachine(instance).compiledClass();

        ClassReader cr = new ClassReader(compiled);
        var out = new ByteArrayOutputStream();
        cr.accept(new TraceClassVisitor(new PrintWriter(out)), 0);

        Approvals.verify(out);
    }
}
