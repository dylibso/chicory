package com.dylibso.chicory.compiler.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

/**
 * Tests that br_on_null correctly refines the type on the fall-through path
 * from nullable (ref null $T) to non-nullable (ref $T).
 *
 * Without the fix in WasmAnalyzer, the AOT compiler fails with:
 * "Expected type Ref[0] <> RefNull[0]"
 */
public class BrOnNullTest {

    @Test
    public void brOnNullRefinesType() {
        var module = Parser.parse(CorpusResources.getResource("compiled/br_on_null.wat.wasm"));

        // This should not throw - the compiler must accept the non-nullable ref
        // on the fall-through path after br_on_null
        var instance =
                assertDoesNotThrow(
                        () ->
                                Instance.builder(module)
                                        .withMachineFactory(MachineFactoryCompiler::compile)
                                        .build());

        // Create a point(42, 7) and verify get_x_or_default returns 42
        var newPoint = instance.export("new_point");
        long[] pointRef = newPoint.apply(42, 7);

        var getX = instance.export("get_x_or_default");
        assertArrayEquals(new long[] {42}, getX.apply(pointRef[0]));
    }

    @Test
    public void brOnNullWithNull() {
        var module = Parser.parse(CorpusResources.getResource("compiled/br_on_null.wat.wasm"));
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();

        // test_null passes ref.null to get_x_or_default, should return -1
        var testNull = instance.export("test_null");
        assertArrayEquals(new long[] {-1}, testNull.apply());
    }
}
