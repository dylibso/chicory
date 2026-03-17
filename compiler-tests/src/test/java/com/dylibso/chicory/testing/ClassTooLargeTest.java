package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.bigWat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.compiler.internal.Compiler;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class ClassTooLargeTest {

    @Test
    public void testFunc50k() {
        var funcCount = 50_000;
        byte[] wasm = Wat2Wasm.parse(bigWat(funcCount, 0));
        var instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withStart(false)
                        .build();

        funcCount = 1000;
        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }

    @Test
    public void testManyBigFuncs() {
        var funcCount = 10;
        byte[] wasm = Wat2Wasm.parse(bigWat(funcCount, 15_000));
        var instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withStart(false)
                        .build();

        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }

    // Takes ~30-50s: generates 33k types to exceed the 65535 constant pool limit,
    // triggering ClassTooLargeException and verifying the call_indirect bridge splitting.
    @Test
    @org.junit.jupiter.api.Disabled
    public void testCallIndirectBridge() {
        byte[] wasm = Wat2Wasm.parse(manyTypesWat(33_000));
        var module = Parser.parse(wasm);

        // Verify bridge classes are generated
        var result = Compiler.builder(module).build().compile();
        var classNames = result.classBytes().keySet();
        assertTrue(
                classNames.stream().anyMatch(k -> k.contains("CallIndirectBridge")),
                "Expected CallIndirectBridge classes to be generated, got: " + classNames);

        // Verify call_indirect works correctly through the bridge
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withStart(false)
                        .build();
        assertEquals(42L, instance.export("main").apply()[0]);
    }

    /**
     * Generates a WAT module with many unique type definitions to trigger
     * ClassTooLargeException on the main class (due to many call_indirect methods),
     * forcing the compiler to split them into bridge classes.
     */
    static String manyTypesWat(int typeCount) {
        String[] valTypes = {"i32", "i64", "f32", "f64"};
        var sb = new StringBuilder();
        sb.append("(module\n");

        // Type 0: the function type used by our test function
        sb.append("  (type (func (result i32)))\n");

        // Types 1..typeCount: unique 8-param types using base-4 encoding
        for (int i = 0; i < typeCount; i++) {
            sb.append("  (type (func (param");
            int n = i;
            for (int j = 0; j < 8; j++) {
                sb.append(" ").append(valTypes[n % 4]);
                n /= 4;
            }
            sb.append(")))\n");
        }

        // Table and element for call_indirect
        sb.append("  (table 1 funcref)\n");
        sb.append("  (elem (i32.const 0) $f0)\n");

        // Function of type 0 that returns 42
        sb.append("  (func $f0 (type 0) i32.const 42)\n");

        // Main function that calls f0 via call_indirect
        sb.append("  (func (export \"main\") (result i32)\n");
        sb.append("    i32.const 0\n");
        sb.append("    call_indirect (type 0)\n");
        sb.append("  )\n");

        sb.append(")\n");
        return sb.toString();
    }
}
