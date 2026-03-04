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
 * Tests for exception handling with extern.wrap (any.convert_extern) in the throw path.
 * Mirrors the exact GraalVM WebImage pattern where exception creation calls a host
 * function (genBacktrace), wraps the externref result via any.convert_extern, stores
 * it in the exception struct, then throws.
 */
public class ExceptionExternWrapTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exception_extern_wrap.wat.wasm"));

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
                                "get_extern",
                                FunctionType.of(List.of(), List.of(ValType.ExternRef)),
                                (inst, args) -> new long[] {0}))
                .build();
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void externWrapCatch(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(42, instance.export("extern-wrap-catch").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void externWrapCallCatch(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(77, instance.export("extern-wrap-call-catch").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void externWrapDeepCatch(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(33, instance.export("extern-wrap-deep-catch").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void externWrapJavacPattern(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(99, instance.export("extern-wrap-javac-pattern").apply()[0]);
    }

    @ParameterizedTest
    @MethodSource("machineImplementations")
    public void externWrapSequential(Function<Instance.Builder, Instance.Builder> machineInject) {
        var instance =
                machineInject
                        .apply(Instance.builder(MODULE).withImportValues(makeImports()))
                        .build();
        assertEquals(30, instance.export("extern-wrap-sequential").apply()[0]);
    }
}
