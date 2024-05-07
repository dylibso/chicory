package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.BitOps.*;

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

    // ========= I32 =========

    @OpCodeIdentifier(OpCode.I32_CLZ)
    public static int I32_CLZ(int tos) {
        return Integer.numberOfLeadingZeros(tos);
    }

    @OpCodeIdentifier(OpCode.I32_CTZ)
    public static int I32_CTZ(int tos) {
        return Integer.numberOfTrailingZeros(tos);
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
        return (int) (asUInt(a) / asUInt(b));
    }

    @OpCodeIdentifier(OpCode.I32_EQ)
    public static int I32_EQ(int b, int a) {
        return a == b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_EQZ)
    public static int I32_EQZ(int a) {
        return a == 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_EXTEND_8_S)
    public static int I32_EXTEND_8_S(int tos) {
        return asByte(tos);
    }

    @OpCodeIdentifier(OpCode.I32_EXTEND_16_S)
    public static int I32_EXTEND_16_S(int tos) {
        var original = tos & 0xFFFF;
        if ((original & 0x8000) != 0) original |= 0xFFFF0000;
        return (int) (original & 0xFFFFFFFFL);
    }

    @OpCodeIdentifier(OpCode.I32_GE_S)
    public static int I32_GE_S(int a, int b) {
        return a >= b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_GE_U)
    public static int I32_GE_U(int a, int b) {
        return Integer.compareUnsigned(a, b) >= 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_GT_S)
    public static int I32_GT_S(int a, int b) {
        return a > b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_GT_U)
    public static int I32_GT_U(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_LE_S)
    public static int I32_LE_S(int a, int b) {
        return a <= b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_LE_U)
    public static int I32_LE_U(int a, int b) {
        return Integer.compareUnsigned(a, b) <= 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_LT_S)
    public static int I32_LT_S(int a, int b) {
        return a < b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_LT_U)
    public static int I32_LT_U(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I32_NE)
    public static int I32_NE(int b, int a) {
        return a == b ? FALSE : TRUE;
    }

    @OpCodeIdentifier(OpCode.I32_POPCNT)
    public static int I32_POPCNT(int tos) {
        return Integer.bitCount(tos);
    }

    @OpCodeIdentifier(OpCode.I32_REM_U)
    public static int I32_REM_U(int a, int b) {
        return (int) (asUInt(a) % asUInt(b));
    }

    @OpCodeIdentifier(OpCode.I32_ROTR)
    public static int I32_ROTR(int v, int c) {
        return (v >>> c) | (v << (32 - c));
    }

    @OpCodeIdentifier(OpCode.I32_ROTL)
    public static int I32_ROTL(int v, int c) {
        return (v << c) | (v >>> (32 - c));
    }

    // ========= F64 =========
    @OpCodeIdentifier(OpCode.F64_CONVERT_I64_U)
    public static double F64_CONVERT_I64_U(long tos) {
        double d;
        if (tos >= 0) {
            d = tos;
        } else {
            // only preserve 53 bits of precision (plus one for rounding) to
            // avoid rounding errors (64 - 53 == 11)
            long sum = tos + 0x3ff;
            // did the add overflow? add the MSB back on after the shift
            long shiftIn = ((sum ^ tos) & Long.MIN_VALUE) >>> 10;
            d = Math.scalb((double) ((sum >>> 11) | shiftIn), 11);
        }
        return Double.doubleToLongBits(d);
    }
}
