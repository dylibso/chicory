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

/**
 * Tests for exception handling with host function callbacks.
 *
 * - Wasm code throws exceptions inside try_table/catch blocks
 * - Catch handlers call imported host functions
 * - Multiple sequential exceptions should each be caught
 */
public class ExceptionCatchHostTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exception_catch_host.wat.wasm"));

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
    public void basicCatch(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(42, instance.export("basic-catch").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void catchCallHost(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(7, instance.export("catch-call-host").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void sequentialCatches(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("sequential-catches").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void nestedCatch(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(99, instance.export("nested-catch").apply()[0]);
    }
}
