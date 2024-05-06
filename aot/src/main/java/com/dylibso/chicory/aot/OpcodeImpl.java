package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.OpCode;

public class OpcodeImpl {

    @OpCodeDecorator(OpCode.I32_ADD)
    public static int I32_ADD(int a, int b) {
        return a + b;
    }
}
