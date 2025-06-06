package com.dylibso.chicory.dwarf.rust;

import static java.lang.ClassLoader.getSystemClassLoader;

import com.dylibso.chicory.compiler.internal.Compiler;
import com.dylibso.chicory.wasm.Parser;
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
    public void verifyCountVowels() {
        verifyGeneratedBytecode("count_vowels.rs.wasm");
    }

    @Test
    public void verifyLogTinyGo() {
        verifyGeneratedBytecode("log.go.tiny.wasm");
    }

    private static void verifyGeneratedBytecode(String name) {
        // skip for now...
        System.out.println("Skipping approval test for " + name);

        var module = Parser.parse(getSystemClassLoader().getResourceAsStream("compiled/" + name));
        var result =
                Compiler.builder(module)
                        .withDebugParser(
                                (x) ->
                                        RustParser.parse(
                                                getSystemClassLoader()
                                                        .getResourceAsStream("compiled/" + name)))
                        .build()
                        .compile();

        Map<String, byte[]> clasess = result.classBytes();
        var className = "com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0";
        verifyClass(Map.of(className, clasess.get(className)));
    }

    public static void verifyClass(Map<String, byte[]> classBytes) {
        var writer = new StringWriter();

        for (byte[] bytes : classBytes.values()) {
            ClassReader cr = new ClassReader(bytes);
            if (!cr.getClassName().endsWith("FuncGroup_0")) {
                continue;
            }
            cr.accept(new TraceClassVisitor(new PrintWriter(writer)), 0);
            writer.append("\n");
        }

        // We are only interested in the debug info at the top of the class
        String output = writer.toString();
        var until = output.indexOf("public static func_0");
        output = output.substring(0, until);
        output = output.strip() + "\n";

        Approvals.verify(output);
    }
}
