package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for exception handling with GC reference payloads. */
public class ExceptionGcRefTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exception_gc_ref.wat.wasm"));

    private static Stream<Arguments> machineImplementations() {
        return Stream.of(
                Arguments.of(
                        (Function<Instance.Builder, Instance.Builder>)
                                (b) -> b.withMachineFactory(InterpreterMachine::new)),
                Arguments.of(
                        (Function<Instance.Builder, Instance.Builder>)
                                (b) -> b.withMachineFactory(MachineFactoryCompiler::compile)));
    }

    private static ImportValues makeImports() {
        return ImportValues.builder()
                .addFunction(
                        new ImportFunction(
                                "host",
                                "on_catch",
                                FunctionType.of(List.of(ValType.I32), List.of(ValType.I32)),
                                (inst, args) -> new long[] {args[0]}))
                .build();
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void basicCatchGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(42, instance.export("basic-catch-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void sequentialCatchesGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("sequential-catches-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void catchFromCallGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("catch-from-call-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void deepCatchGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("deep-catch-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void catchInLoopGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(4, instance.export("catch-in-loop-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void deepCatchInLoopGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(4, instance.export("deep-catch-in-loop-gc").apply()[0]);
    }
}
