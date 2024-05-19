package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.BitOps.FALSE;
import static com.dylibso.chicory.runtime.BitOps.TRUE;

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
 *
 * Note about parameter ordering: because of the JVM's calling convention, the parameters to a method
 * are ordered such that the last value pushed is the last argument to the method, i.e.,
 * method(tos - 2, tos - 1, tos).
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
        return Integer.divideUnsigned(a, b);
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
        return (byte) tos;
    }

    @OpCodeIdentifier(OpCode.I32_EXTEND_16_S)
    public static int I32_EXTEND_16_S(int tos) {
        return (short) tos;
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
        return Integer.remainderUnsigned(a, b);
    }

    @OpCodeIdentifier(OpCode.I32_ROTR)
    public static int I32_ROTR(int v, int c) {
        return (v >>> c) | (v << (32 - c));
    }

    @OpCodeIdentifier(OpCode.I32_ROTL)
    public static int I32_ROTL(int v, int c) {
        return (v << c) | (v >>> (32 - c));
    }

    // ========= I64 =========
    @OpCodeIdentifier(OpCode.I64_CLZ)
    public static long I64_CLZ(long tos) {
        return Long.numberOfLeadingZeros(tos);
    }

    @OpCodeIdentifier(OpCode.I64_CTZ)
    public static long I64_CTZ(long tos) {
        return Long.numberOfTrailingZeros(tos);
    }

    @OpCodeIdentifier(OpCode.I64_DIV_S)
    public static long I64_DIV_S(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new WASMRuntimeException("integer overflow");
        }
        return a / b;
    }

    @OpCodeIdentifier(OpCode.I64_DIV_U)
    public static long I64_DIV_U(long a, long b) {
        return Long.divideUnsigned(a, b);
    }

    @OpCodeIdentifier(OpCode.I64_EQ)
    public static int I64_EQ(long b, long a) {
        return a == b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_EQZ)
    public static int I64_EQZ(long a) {
        return a == 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_EXTEND_8_S)
    public static long I64_EXTEND_8_S(long tos) {
        return (byte) tos;
    }

    @OpCodeIdentifier(OpCode.I64_EXTEND_16_S)
    public static long I64_EXTEND_16_S(long tos) {
        return (short) tos;
    }

    @OpCodeIdentifier(OpCode.I64_EXTEND_32_S)
    public static long I64_EXTEND_32_S(long tos) {
        return (int) tos;
    }

    @OpCodeIdentifier(OpCode.I64_GE_S)
    public static int I64_GE_S(long a, long b) {
        return a >= b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_GE_U)
    public static int I64_GE_U(long a, long b) {
        return Long.compareUnsigned(a, b) >= 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_GT_S)
    public static int I64_GT_S(long a, long b) {
        return a > b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_GT_U)
    public static int I64_GT_U(long a, long b) {
        return Long.compareUnsigned(a, b) > 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_LE_S)
    public static int I64_LE_S(long a, long b) {
        return a <= b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_LE_U)
    public static int I64_LE_U(long a, long b) {
        return Long.compareUnsigned(a, b) <= 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_LT_S)
    public static int I64_LT_S(long a, long b) {
        return a < b ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_LT_U)
    public static int I64_LT_U(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? TRUE : FALSE;
    }

    @OpCodeIdentifier(OpCode.I64_NE)
    public static int I64_NE(long b, long a) {
        return a == b ? FALSE : TRUE;
    }

    @OpCodeIdentifier(OpCode.I64_POPCNT)
    public static long I64_POPCNT(long tos) {
        return Long.bitCount(tos);
    }

    @OpCodeIdentifier(OpCode.I64_REM_U)
    public static long I64_REM_U(long a, long b) {
        return Long.remainderUnsigned(a, b);
    }

    @OpCodeIdentifier(OpCode.I64_ROTR)
    public static long I64_ROTR(long v, long c) {
        return (v >>> c) | (v << (64 - c));
    }

    @OpCodeIdentifier(OpCode.I64_ROTL)
    public static long I64_ROTL(long v, long c) {
        return (v << c) | (v >>> (64 - c));
    }

    // ========= F64 =========
    @OpCodeIdentifier(OpCode.F64_CONVERT_I64_U)
    public static double F64_CONVERT_I64_U(long tos) {
        if (tos >= 0) {
            return tos;
        }
        // only preserve 53 bits of precision (plus one for rounding) to
        // avoid rounding errors (64 - 53 == 11)
        long sum = tos + 0x3ff;
        // did the add overflow? add the MSB back on after the shift
        long shiftIn = ((sum ^ tos) & Long.MIN_VALUE) >>> 10;
        return Math.scalb((double) ((sum >>> 11) | shiftIn), 11);
    }
}
