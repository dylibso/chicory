package com.dylibso.chicory.testing;

import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.types.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddTest {

    private static final String ADD_WAT =
            "(module (func (export \"add\") (param $x i32) (param $y i32) (result i32) (i32.add (local.get $x) (local.get $y))))";

    @Test
    public void testAdd() {
        var moduleInstance = Module
                .builder(Wat2Wasm.parse(ADD_WAT))
                .build()
                .withTypeValidation(true)
                .instantiate()
                .initialize(true);

        var addFunction = moduleInstance.export("add");
        var results = addFunction.apply(Value.i32(Integer.parseUnsignedInt("1")), Value.i32(Integer.parseUnsignedInt("41")));
        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
    }
}
