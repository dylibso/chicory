package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that br_on_null correctly refines the type on the fall-through path
 * from nullable (ref null $T) to non-nullable (ref $T).
 */
public class BrOnNullTest {

    private static final WasmModule module =
            Parser.parse(CorpusResources.getResource("compiled/br_on_null.wat.wasm"));

    private static Stream<Arguments> machineImplementations() {
        return Stream.of(
                Arguments.of(
                        (Function<Instance.Builder, Instance.Builder>)
                                (b) -> b.withMachineFactory(InterpreterMachine::new)),
                Arguments.of(
                        (Function<Instance.Builder, Instance.Builder>)
                                (b) -> b.withMachineFactory(MachineFactoryCompiler::compile)));
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void brOnNullRefinesType(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance = machineInject.apply(Instance.builder(module)).build();

        // Create a point(42, 7) and verify get_x_or_default returns 42
        var newPoint = instance.export("new_point");
        long[] pointRef = newPoint.apply(42, 7);

        var getX = instance.export("get_x_or_default");
        assertEquals(42, getX.apply(pointRef[0])[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void brOnNullWithNull(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance = machineInject.apply(Instance.builder(module)).build();

        // test_null passes ref.null to get_x_or_default, should return -1
        var testNull = instance.export("test_null");
        assertEquals(-1, testNull.apply()[0]);
    }
}
