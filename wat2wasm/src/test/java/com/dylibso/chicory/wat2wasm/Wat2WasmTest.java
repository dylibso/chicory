package com.dylibso.chicory.wat2wasm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.exceptions.MalformedException;
import java.io.File;
import org.junit.jupiter.api.Test;

public class Wat2WasmTest {

    @Test
    public void shouldRunWat2Wasm() throws Exception {
        var result = Wat2Wasm.parse(new File("../wasm-corpus/src/test/resources/wat/iterfact.wat"));

        assertTrue(result.length > 0);
        assertTrue(new String(result).contains("iterFact"));
    }

    @Test
    public void shouldThrowMalformedException() throws Exception {
        var malformedException =
                assertThrows(
                        MalformedException.class,
                        () ->
                                Wat2Wasm.parse(
                                        new File(
                                                "src/test/resources/utf8-invalid-encoding-spec.0.wat")));

        assertTrue(malformedException.getMessage().contains("invalid utf-8 encoding"));
    }
}
