package com.dylibso.chicory.compiler.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class CallTest {

    @Test
    public void callLotsOfArgs() throws InterruptedException {
        var module = Parser.parse(CorpusResources.getResource("compiled/lots-of-args.wat.wasm"));
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();

        var function = instance.export("test");
        long[] result = function.apply(2, 3);
        assertArrayEquals(new long[] {5}, result);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void callLotsOfArgsOnDeprecatedAotMachine() throws InterruptedException {
        var module = Parser.parse(CorpusResources.getResource("compiled/lots-of-args.wat.wasm"));
        var instance =
                Instance.builder(module)
                        .withMachineFactory(MachineFactoryCompiler::compile)
                        .build();

        var function = instance.export("test");
        long[] result = function.apply(2, 3);
        assertArrayEquals(new long[] {5}, result);
    }
}
