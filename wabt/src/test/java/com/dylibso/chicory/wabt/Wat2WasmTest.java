package com.dylibso.chicory.wabt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.corpus.WatGenerator;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasm.Parser;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class Wat2WasmTest {

    @Test
    public void shouldRunWat2Wasm() throws Exception {
        var result = Wat2Wasm.parse(new File("../wasm-corpus/src/main/resources/wat/iterfact.wat"));

        assertTrue(result.length > 0);
        assertTrue(new String(result, UTF_8).contains("iterFact"));
    }

    @Test
    public void shouldRunWat2WasmOnString() {
        var moduleInstance =
                Instance.builder(
                                Parser.parse(
                                        Wat2Wasm.parse(
                                                "(module (func (export \"add\") (param $x"
                                                        + " i32) (param $y i32) (result i32)"
                                                        + " (i32.add (local.get $x) (local.get"
                                                        + " $y))))")))
                        .withInitialize(true)
                        .build();

        var addFunction = moduleInstance.export("add");
        var results = addFunction.apply(1, 41);
        assertEquals(42L, results[0]);
    }

    @Test
    public void shouldThrowMalformedException() throws Exception {
        var exitException =
                assertThrows(
                        WasiExitException.class,
                        () ->
                                Wat2Wasm.parse(
                                        new File(
                                                "src/test/resources/utf8-invalid-encoding-spec.0.wat")));

        assertEquals(1, exitException.exitCode());
    }

    @Test
    public void canCompile50kFunctions() throws IOException {
        String wat = WatGenerator.bigWat(50_000, 0);
        Wat2Wasm.parse(wat);
    }

    @Test
    public void canCompileBigFunctions() throws IOException {
        String wat = WatGenerator.bigWat(10, 15_000);
        Wat2Wasm.parse(wat);
    }
}
