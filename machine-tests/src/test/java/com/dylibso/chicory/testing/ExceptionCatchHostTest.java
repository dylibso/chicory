package com.dylibso.chicory.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.ImportFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for exception handling with host function callbacks.
 *
 * Reproduces a pattern from GraalVM WebImage javac-in-wasm where:
 * - Wasm code throws exceptions inside try_table/catch blocks
 * - Catch handlers call imported host functions
 * - Multiple sequential exceptions should each be caught
 *
 * In Node.js these exceptions are caught correctly; this test
 * verifies the same behavior in Chicory's interpreter and compiler.
 */
public class ExceptionCatchHostTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exception_catch_host.wat.wasm"));

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

    // --- Interpreter tests ---

    @Test
    public void basicCatchInterpreted() {
        var instance = Instance.builder(MODULE).withImportValues(makeImports()).build();
        assertArrayEquals(new long[] {42}, instance.export("basic-catch").apply());
    }

    @Test
    public void catchCallHostInterpreted() {
        var instance = Instance.builder(MODULE).withImportValues(makeImports()).build();
        assertArrayEquals(new long[] {7}, instance.export("catch-call-host").apply());
    }

    @Test
    public void sequentialCatchesInterpreted() {
        var instance = Instance.builder(MODULE).withImportValues(makeImports()).build();
        assertArrayEquals(new long[] {30}, instance.export("sequential-catches").apply());
    }

    @Test
    public void nestedCatchInterpreted() {
        var instance = Instance.builder(MODULE).withImportValues(makeImports()).build();
        assertArrayEquals(new long[] {99}, instance.export("nested-catch").apply());
    }

    // --- Compiler tests ---

    @Test
    public void basicCatchCompiled() {
        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withImportValues(makeImports())
                        .build();
        assertArrayEquals(new long[] {42}, instance.export("basic-catch").apply());
    }

    @Test
    public void catchCallHostCompiled() {
        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withImportValues(makeImports())
                        .build();
        assertArrayEquals(new long[] {7}, instance.export("catch-call-host").apply());
    }

    @Test
    public void sequentialCatchesCompiled() {
        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withImportValues(makeImports())
                        .build();
        assertArrayEquals(new long[] {30}, instance.export("sequential-catches").apply());
    }

    @Test
    public void nestedCatchCompiled() {
        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .withImportValues(makeImports())
                        .build();
        assertArrayEquals(new long[] {99}, instance.export("nested-catch").apply());
    }
}
