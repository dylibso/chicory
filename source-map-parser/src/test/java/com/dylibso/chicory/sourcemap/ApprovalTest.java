package com.dylibso.chicory.sourcemap;

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
    public void verifyCountVowels() {
        verifyGeneratedBytecode("count_vowels.rs.wasm");
    }

    @Test
    public void verifyLogTinyGo() {
        verifyGeneratedBytecode("log.go.tiny.wasm");
    }

    private static void verifyGeneratedBytecode(String name) {
        try {
            var module = parse(getSystemClassLoader().getResourceAsStream("compiled/" + name));
            var sourceMap =
                    SourceMapParser.parse(
                            getSystemClassLoader().getResourceAsStream("compiled/" + name));
            var result = AotCompiler.builder(module).withSourceMap(sourceMap).build().compile();
            Map<String, byte[]> clasess = result.classBytes();
            name = "com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0";
            verifyClass(Map.of(name, clasess.get(name)), true);
        } catch (SourceMapException e) {
            throw new RuntimeException(e);
        }
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

        // We are only interested in the debug info at the top of the class
        String output = writer.toString();
        var until = output.indexOf("public static func_0");
        output = output.substring(0, until);
        output = output.strip() + "\n";

        Approvals.verify(output);

        assertFalse(
                output.contains(getInternalName(AotMethods.class)),
                "Class contains non-inlined reference to AotMethods");
    }
}
