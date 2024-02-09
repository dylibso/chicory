package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.ValueType.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FunctionTypeTest {
    @Test
    public void toStringContract() {
        var emptyToNil = new FunctionType(new ValueType[0], new ValueType[0]);
        assertEquals("() -> nil", emptyToNil.toString());

        var i32I64ToF32 = new FunctionType(new ValueType[] {I32, I64}, new ValueType[] {F32});
        assertEquals("(I32,I64) -> F32", i32I64ToF32.toString());

        var v128ToI32I32 = new FunctionType(new ValueType[] {V128}, new ValueType[] {I32, I32});
        assertEquals("(V128) -> I32,I32", v128ToI32I32.toString());
    }
}
