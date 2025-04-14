package com.dylibso.chicory.approvals;

import static com.dylibso.chicory.wasm.Parser.parse;
import static java.lang.ClassLoader.getSystemClassLoader;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.experimental.aot.AotMethods;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

// To approve everything use the env var: `APPROVAL_TESTS_USE_REPORTER=AutoApproveReporter`
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
    public void verifyLotsOfArgs() {
        verifyGeneratedBytecode("lots-of-args.wat.wasm");
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
    public void verifyI32Renamed() {
        var module = parse(getSystemClassLoader().getResourceAsStream("compiled/i32.wat.wasm"));
        var result = AotCompiler.builder(module).withClassName("FOO").build().compile();
        verifyClass(result.classBytes(), false);
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
        var module = parse(getSystemClassLoader().getResourceAsStream("compiled/" + name));
        var result = AotCompiler.builder(module).build().compile();
        verifyClass(result.classBytes(), true);
    }

    private static void verifyClass(Map<String, byte[]> classBytes, boolean skipAotMethods) {
        var writer = new StringWriter();

        for (byte[] bytes : classBytes.values()) {
            ClassReader cr = new ClassReader(bytes);
            if (skipAotMethods && cr.getClassName().endsWith("$AotMethods")) {
                continue;
            }
            cr.accept(new TraceClassVisitor(new PrintWriter(writer)), 0);
            writer.append("\n");
        }

        String output = writer.toString();
        output = output.replaceAll("(?m)^ {3}FRAME.*\\n", "");
        output = output.replaceAll("(?m)^ {4}MAX(STACK|LOCALS) = \\d+\\n", "");
        output = output.replaceAll("(?m)^ {4}(LINENUMBER|LOCALVARIABLE) .*\\n", "");
        output = output.replaceAll("(?m)^ *// .*\\n", "");
        output = output.replaceAll("\\n{3,}", "\n\n");
        output = output.strip() + "\n";

        Approvals.verify(output);

        assertFalse(
                output.contains(getInternalName(AotMethods.class)),
                "Class contains non-inlined reference to AotMethods");
    }
}
