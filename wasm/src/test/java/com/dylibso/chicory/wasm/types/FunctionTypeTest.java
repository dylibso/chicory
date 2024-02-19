package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.ValueType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FunctionTypeTest {
    @Test
    public void toStringContract() {
        var emptyToNil = FunctionType.empty();
        assertEquals("() -> nil", emptyToNil.toString());

        var i32I64ToF32 = FunctionType.of(List.of(I32, I64), List.of(F32));
        assertEquals("(I32,I64) -> (F32)", i32I64ToF32.toString());

        var v128ToI32I32 = FunctionType.of(List.of(V128), List.of(I32, I32));
        assertEquals("(V128) -> (I32,I32)", v128ToI32I32.toString());
    }
}
