package com.dylibso.chicory.wabt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import org.junit.jupiter.api.Test;

public class Wat2WasmTest {

    @Test
    public void shouldRunWat2Wasm() throws Exception {
        var result = Wat2Wasm.parse(new File("../wasm-corpus/src/main/resources/wat/iterfact.wat"));

        assertTrue(result.length > 0);
        assertTrue(new String(result).contains("iterFact"));
    }

    @Test
    public void shouldRunWat2WasmOnString() {
        var moduleInstance = Module.builder(Wat2Wasm.parse("(module (func (export \"add\") (param $x i32) (param $y"
                        + " i32) (result i32) (i32.add (local.get $x)"
                        + " (local.get $y))))"))
                .withTypeValidation(true)
                .withInitialize(true)
                .build()
                .instantiate();

        var addFunction = moduleInstance.export("add");
        var results =
                addFunction.apply(Value.i32(Integer.parseUnsignedInt("1")), Value.i32(Integer.parseUnsignedInt("41")));
        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
    }

    @Test
    public void shouldThrowMalformedException() throws Exception {
        var malformedException = assertThrows(
                MalformedException.class,
                () -> Wat2Wasm.parse(new File("src/test/resources/utf8-invalid-encoding-spec.0.wat")));

        assertTrue(malformedException.getMessage().contains("invalid utf-8 encoding"));
    }
}
