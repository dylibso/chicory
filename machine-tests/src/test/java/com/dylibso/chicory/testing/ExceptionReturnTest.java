package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for exception handling when `return` instruction is used in the call chain. */
public class ExceptionReturnTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exception_return.wat.wasm"));

    private static Stream<Arguments> machineImplementations() {
        return Stream.of(
                Arguments.of(
                        (Function<Instance.Builder, Instance.Builder>)
                                (b) -> b.withMachineFactory(InterpreterMachine::new)));
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void catchAfterReturn(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(
                                Instance.builder(MODULE)
                                        .withImportValues(ImportValues.builder().build()))
                        .build();
        assertEquals(42, instance.export("catch-after-return").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void catchSequentialWithReturn(
            Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(
                                Instance.builder(MODULE)
                                        .withImportValues(ImportValues.builder().build()))
                        .build();
        assertEquals(30, instance.export("catch-sequential-with-return").apply()[0]);
    }
}
