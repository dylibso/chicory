package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.BitOps.asUInt;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.OpCode;

/**
 * Note: Some opcodes are easy or trivial to implement as compiler intrinsics (local.get, i32.add, etc).
 * Others would be very difficult to implement and maintain (floating point truncations, for example).
 * The idea of this class is to share the core logic of both the interpreter & AOT implementations for
 * shareable opcodes (that is, opcodes that are not completely different in operation depending on
 * whether they're run in the interpreter or in the AOT, such as local.get, local.set, etc) in a
 * single place that is statically accessible. If the AOT does not have an intrinsic for an opcode (and
 * the opcode is not a flow control opcode), then a static call will be generated to the method in this
 * class that implements the opcode.
 */
public class OpcodeImpl {

    @OpCodeIdentifier(OpCode.I32_ADD)
    public static int I32_ADD(int a, int b) {
        return a + b;
    }

    @OpCodeIdentifier(OpCode.I32_SUB)
    public static int I32_SUB(int a, int b) {
        return a - b;
    }

    @OpCodeIdentifier(OpCode.I32_MUL)
    public static int I32_MUL(int a, int b) {
        return a * b;
    }

    @OpCodeIdentifier(OpCode.I32_DIV_S)
    public static int I32_DIV_S(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new WASMRuntimeException("integer overflow");
        }
        return a / b;
    }

    @OpCodeIdentifier(OpCode.I32_DIV_U)
    public static int I32_DIV_U(int a, int b) {
        return a / b;
    }

    @OpCodeIdentifier(OpCode.I32_REM_S)
    public static int I32_REM_S(int a, int b) {
        return a % b;
    }

    @OpCodeIdentifier(OpCode.I32_REM_U)
    public static int I32_REM_U(int a, int b) {
        return (int) (asUInt(a) % asUInt(b));
    }
}
