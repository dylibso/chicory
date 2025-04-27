package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.bigWat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.experimental.aot.AotMethods;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class MethodTooLargeTest {

    @Test
    public void testBigFunc() {
        byte[] wasm = Wat2Wasm.parse(bigWat(1, 20_000));

        var module = Parser.parse(wasm);
        var result = AotCompiler.builder(module).build().compile();

        verifyClass(result.classBytes(), true);

        var instance =
                Instance.builder(module)
                        .withMachineFactory(result.machineFactory())
                        .withStart(false)
                        .build();

        ExportFunction func1 = instance.export("func_" + 1);
        assertEquals(1, func1.apply(0)[0]);
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
