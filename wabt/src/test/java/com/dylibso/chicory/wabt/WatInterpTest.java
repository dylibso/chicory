package com.dylibso.chicory.wabt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.runtime.Module;
import java.io.File;
import org.junit.jupiter.api.Test;

public class WatInterpTest {

    @Test
    public void shouldRunWasmInterp() throws Exception {
        var module =
                WasmInterp.builder()
                        .withFile(
                                new File("../wasm-corpus/src/main/resources/compiled/add.wat.wasm"))
                        .build();

        var result = module.invoke("add", "i32:1", "i32:2");

        assertEquals("add(i32:1, i32:2) => i32:3", result.trim());
    }

    @Test
    public void shouldRunAFuzzReproducer() throws Exception {
        var wasmInterpModule =
                WasmInterp.builder()
                        .withFile(
                                new File(
                                        "../wasm-corpus/src/main/resources/compiled/fuzz-crash-repro-y4ye0lx.wat.wasm"))
                        .build();
        // repro() => f64:10318438020585182.000000, f32:4668146927244482561527775232.000000
        var wasmInterpResult = wasmInterpModule.invoke("repro").trim();

        var chicoryModule =
                Module.builder(
                                new File(
                                        "../wasm-corpus/src/main/resources/compiled/fuzz-crash-repro-y4ye0lx.wat.wasm"))
                        .build();
        var chicoryResult = chicoryModule.instantiate().export("repro").apply();

        var splitted = wasmInterpResult.split("\\:");
        var firstValue = splitted[1].split("\\,")[0];
        var secondValue = splitted[2];
        var doubleValue = Double.valueOf(firstValue);
        var floatValue = Float.valueOf(secondValue);

        assertEquals(doubleValue, chicoryResult[0].asDouble());
        assertEquals(floatValue, chicoryResult[1].asFloat());
    }
}
