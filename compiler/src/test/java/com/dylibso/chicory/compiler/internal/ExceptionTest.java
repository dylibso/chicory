package com.dylibso.chicory.compiler.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.HashMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

    private static final WasmModule MODULE =
            Parser.parse(CorpusResources.getResource("compiled/exceptions.wat.wasm"));

    private static int throwIfFn;
    private static int catchlessTryFn;

    static {
        var indexes = getFunctionNameIndex(MODULE, "throw-if", "catchless-try");
        ExceptionTest.throwIfFn = indexes[0];
        ExceptionTest.catchlessTryFn = indexes[1];
    }

    public static int[] getFunctionNameIndex(WasmModule module, String... functionNames) {
        HashMap<String, Integer> functionNameIndex = new HashMap<>();
        for (int i = 0; i < MODULE.functionSection().functionCount(); i++) {
            functionNameIndex.put(MODULE.nameSection().nameOfFunction(i), i);
        }

        var result = new int[functionNames.length];
        for (int i = 0; i < functionNames.length; i++) {
            Integer index = functionNameIndex.get(functionNames[i]);
            if (index == null) {
                throw new IllegalArgumentException(
                        "Function " + functionNames[i] + " not found in module");
            }
            result[i] = index;
        }
        return result;
    }

    @Test
    public void throwFromInterpretedCatchCompiled() throws InterruptedException {

        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(MODULE)
                                        .withInterpretedFunctions(Set.of(throwIfFn))
                                        .compile())
                        .build();

        var function = instance.export("catchless-try");
        assertArrayEquals(new long[] {0}, function.apply(0));
        assertArrayEquals(new long[] {1}, function.apply(1));
    }

    @Test
    public void throwFromCompiledCatchInterpreted() throws InterruptedException {

        var instance =
                Instance.builder(MODULE)
                        .withMachineFactory(
                                MachineFactoryCompiler.builder(MODULE)
                                        .withInterpretedFunctions(Set.of(catchlessTryFn))
                                        .compile())
                        .build();

        var function = instance.export("catchless-try");
        assertArrayEquals(new long[] {0}, function.apply(0));
        assertArrayEquals(new long[] {1}, function.apply(1));
    }
}
