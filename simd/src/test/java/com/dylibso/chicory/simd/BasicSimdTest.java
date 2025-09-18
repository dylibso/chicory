package com.dylibso.chicory.simd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class BasicSimdTest {

    @Test
    public void shouldRunBasicExample() {
        // from: https://blog.dkwr.de/development/wasm-simd-operations/
        var instance =
                Instance.builder(
                                Parser.parse(
                                        CorpusResources.getResource(
                                                "compiled/simd-example.wat.wasm")))
                        .withMachineFactory(SimdInterpreterMachine::new)
                        .build();
        var main = instance.export("main");
        var result = main.apply()[0];
        assertEquals(6L, result);
    }
}
