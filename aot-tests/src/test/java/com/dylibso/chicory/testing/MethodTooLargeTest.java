package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.methodTooLarge;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.compiler.internal.Compiler;
import com.dylibso.chicory.compiler.internal.Methods;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

public class MethodTooLargeTest {

    @Test
    public void testBigFunc() {
        String wat = methodTooLarge(20_000);
        byte[] wasm = Wat2Wasm.parse(wat);

        var module = Parser.parse(wasm);
        var result = Compiler.builder(module).build().compile();

        // We only verify that the resulting class contains the fallback to interpreter
        verifyClass(result.classBytes(), true);
    }

    private static void verifyClass(Map<String, byte[]> classBytes, boolean skipAotMethods) {
        var writer = new StringWriter();

        for (byte[] bytes : classBytes.values()) {
            ClassReader cr = new ClassReader(bytes);
            if (skipAotMethods && cr.getClassName().endsWith("$AotMethods")) {
                continue;
            }
            cr.accept(
                    new ClassVisitor(Opcodes.ASM9, new TraceClassVisitor(new PrintWriter(writer))) {
                        @Override
                        public void visit(
                                int version,
                                int access,
                                String name,
                                String signature,
                                String superName,
                                String[] interfaces) {
                            if (name.endsWith("CompiledMachineFuncGroup_0")) {
                                super.visit(
                                        version, access, name, signature, superName, interfaces);
                            }
                        }

                        @Override
                        public MethodVisitor visitMethod(
                                int access,
                                String name,
                                String descriptor,
                                String signature,
                                String[] exceptions) {
                            if (name.equals("func_2")) {
                                return super.visitMethod(
                                        access, name, descriptor, signature, exceptions);
                            }
                            return null;
                        }
                    },
                    0);
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
                output.contains(getInternalName(Methods.class)),
                "Class contains non-inlined reference to AotMethods");
    }
}
