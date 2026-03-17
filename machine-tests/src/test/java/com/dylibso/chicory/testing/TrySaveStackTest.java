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
 * Tests that values below a try_table scope are preserved when a catch fires.
 * This exercises the TRY_SAVE_STACK / TRY_RESTORE_STACK mechanism in the compiler.
 */
public class TrySaveStackTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/try_save_stack.wat.wasm"));

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
    public void valueBelowTry(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance = machineInject.apply(Instance.builder(MODULE)).build();
        assertEquals(42, instance.export("value-below-try").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void twoValuesBelowTry(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance = machineInject.apply(Instance.builder(MODULE)).build();
        assertEquals(305, instance.export("two-values-below-try").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void nestedTryValues(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance = machineInject.apply(Instance.builder(MODULE)).build();
        assertEquals(6, instance.export("nested-try-values").apply()[0]);
    }
}
