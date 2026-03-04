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
 * Tests for exception handling with GC reference payloads.
 *
 * GraalVM WebImage compiles Java exceptions to Wasm using:
 *   (tag $tag0 (param (ref null $Throwable)))
 * This test verifies that try_table/catch works correctly when the
 * exception tag carries a GC struct reference (not just i32).
 */
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
        // Loop catches 4 exceptions (i=0,1,2,3), then i=4 doesn't throw
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

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void indirectCatchGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(42, instance.export("indirect-catch-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void indirectSequentialGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("indirect-sequential-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void indirectLoopGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(4, instance.export("indirect-loop-gc").apply()[0]);
    }

    // --- GC subtype exception tests ---
    // In GraalVM WebImage: tag has (param (ref null $Throwable)) but thrown value
    // is a subtype like NoSuchFileException. These tests verify that catch works
    // when the thrown struct is a subtype of the tag's declared parameter type.

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void subtypeCatchGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(55, instance.export("subtype-catch-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void subtypeFromCallGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(77, instance.export("subtype-from-call-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void subtypeDeepCallGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(33, instance.export("subtype-deep-call-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void subtypeSequentialGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("subtype-sequential-gc").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void subtypeLoopDeepGc(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(4, instance.export("subtype-loop-deep-gc").apply()[0]);
    }
}
