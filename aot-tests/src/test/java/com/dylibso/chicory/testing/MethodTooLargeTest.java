package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.methodTooLarge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.experimental.aot.AotCompiler;
import com.dylibso.chicory.experimental.aot.AotMethods;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class MethodTooLargeTest {

    @Test
    public void testBigFunc() {
        String wat = methodTooLarge(20_000);
        byte[] wasm = Wat2Wasm.parse(wat);

        var module = Parser.parse(wasm);
        var result = AotCompiler.builder(module).build().compile();

        var hostStackTrace = new ArrayList<String>();
        var hostFunc =
                new HostFunction(
                        "funcs",
                        "host_func",
                        List.of(ValueType.I32),
                        List.of(ValueType.I32),
                        (inst, args) -> {
                            var thread = Thread.currentThread();
                            int i = 0;
                            for (StackTraceElement element : thread.getStackTrace()) {
                                i++;
                                if (i < 2 || i > 19) {
                                    continue;
                                }
                                hostStackTrace.add(
                                        element.getClassName() + "." + element.getMethodName());
                            }
                            return new long[] {35};
                        });

        var instance =
                Instance.builder(module)
                        .withImportValues(ImportValues.builder().addFunction(hostFunc).build())
                        .withMachineFactory(result.machineFactory())
                        .withStart(false)
                        .build();

        verifyClass(result.classBytes(), true);

        assertEquals(35, instance.export("func_2").apply(0)[0]);

        assertEquals(
                List.of(
                        "com.dylibso.chicory.testing.MethodTooLargeTest.lambda$testBigFunc$0",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.runtime.InterpreterMachine.CALL",
                        "com.dylibso.chicory.runtime.InterpreterMachine.eval",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.runtime.InterpreterMachine.CALL",
                        "com.dylibso.chicory.runtime.InterpreterMachine.eval",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.runtime.InterpreterMachine.call",
                        "com.dylibso.chicory.$gen.CompiledMachine.call",
                        "com.dylibso.chicory.$gen.CompiledMachine$AotMethods.callIndirect",
                        // here is where the AOT method switches to the interpreter, would be nice
                        // if we can get the interpreter to switch back to AOT for the call to
                        // func_1
                        "com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.func_2",
                        "com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.func_3",
                        "com.dylibso.chicory.$gen.CompiledMachineFuncGroup_0.call_3",
                        "com.dylibso.chicory.$gen.CompiledMachine$MachineCall.call",
                        "com.dylibso.chicory.$gen.CompiledMachine.call",
                        "com.dylibso.chicory.runtime.Instance$Exports.lambda$function$0",
                        "com.dylibso.chicory.testing.MethodTooLargeTest.testBigFunc"),
                hostStackTrace);
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
