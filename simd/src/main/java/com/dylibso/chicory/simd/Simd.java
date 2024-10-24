package com.dylibso.chicory.simd;

import static com.dylibso.chicory.runtime.Machine.readMemPtr;

import com.dylibso.chicory.runtime.BitOps;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.HashMap;
import java.util.Map;
import jdk.incubator.vector.LongVector;

public final class Simd {

    private Simd() {}

    public static Map<OpCode, Machine.OpImpl> opcodesImpl = new HashMap<>();

    static {
        opcodesImpl.put(OpCode.V128_CONST, Simd::V128_CONST);
        opcodesImpl.put(OpCode.V128_LOAD, Simd::V128_LOAD);
        opcodesImpl.put(OpCode.I8x16_EXTRACT_LANE_S, Simd::I8x16_EXTRACT_LANE_S);
        opcodesImpl.put(OpCode.V128_NOT, Simd::V128_NOT);
        opcodesImpl.put(OpCode.V128_BITSELECT, Simd::V128_BITSELECT);
        opcodesImpl.put(OpCode.I8x16_EQ, Simd::I8x16_EQ);
        opcodesImpl.put(OpCode.I8x16_SUB, Simd::I8x16_SUB);
        opcodesImpl.put(OpCode.I8x16_ADD, Simd::I8x16_ADD);
        opcodesImpl.put(OpCode.I8x16_SWIZZLE, Simd::I8x16_SWIZZLE);
        opcodesImpl.put(OpCode.I8x16_ALL_TRUE, Simd::I8x16_ALL_TRUE);
        opcodesImpl.put(OpCode.I8x16_SHL, Simd::I8x16_SHL);
        opcodesImpl.put(OpCode.F32x4_MUL, Simd::F32x4_MUL);
        opcodesImpl.put(OpCode.F32x4_ABS, Simd::F32x4_ABS);
        opcodesImpl.put(OpCode.F32x4_MIN, Simd::F32x4_MIN);
        opcodesImpl.put(OpCode.I32x4_TRUNC_SAT_F32X4_S, Simd::I32x4_TRUNC_SAT_F32x4_S);
        opcodesImpl.put(OpCode.F32x4_CONVERT_I32x4_U, Simd::F32x4_CONVERT_I32x4_U);
    }

    private static void V128_CONST(MStack stack, Instance instance, Machine.Operands operands) {
        stack.push(operands.get(0));
        stack.push(operands.get(1));
    }

    private static void V128_LOAD(MStack stack, Instance instance, Machine.Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var valHigh = instance.memory().readLong(ptr);
        var valLow = instance.memory().readLong(ptr + 8);
        stack.push(valHigh);
        stack.push(valLow);
    }

    private static void I8x16_EXTRACT_LANE_S(
            MStack stack, Instance instance, Machine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();
        long result;
        var laneIdx = operands.get(0);
        if (laneIdx < 8) {
            result = (int) (valLow >> (laneIdx * 8) & 0xFF);
        } else {
            result = (int) (valHigh >> (laneIdx * 8) & 0xFF);
        }
        stack.push(result);
    }

    private static void I8x16_EQ(MStack stack, Instance instance, Machine.Operands operands) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        // TODO: refactor in a more generic operation?
        for (int i = 0; i < 8; i++) {
            var shift = i * 8L;
            resultHigh |=
                    (((val1High >> shift) & 0xFFL) == ((val2High >> shift) & 0xFFL))
                            ? (0xFFL << shift)
                            : 0;
            resultLow |=
                    (((val1Low >> shift) & 0xFFL) == ((val2Low >> shift) & 0xFFL))
                            ? (0xFFL << shift)
                            : 0;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_ADD(MStack stack, Instance instance, Machine.Operands operands) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 8; i++) {
            var shift = i * 8L;
            resultHigh |=
                    ((((val2High >> shift) & 0xFFL) + ((val1High >> shift) & 0xFFL)) & 0xFFL)
                            << shift;
            resultLow |=
                    ((((val2Low >> shift) & 0xFFL) + ((val1Low >> shift) & 0xFFL)) & 0xFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_SUB(MStack stack, Instance instance, Machine.Operands operands) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 8; i++) {
            var shift = i * 8L;
            resultHigh |=
                    ((((val2High >> shift) & 0xFFL) - ((val1High >> shift) & 0xFFL)) & 0xFFL)
                            << shift;
            resultLow |=
                    ((((val2Low >> shift) & 0xFFL) - ((val1Low >> shift) & 0xFFL)) & 0xFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_SHL(MStack stack, Instance instance, Machine.Operands operands) {
        var s = stack.pop();
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;
        for (int i = 0; i < 8; i++) {
            var shift = i * 8L;
            resultHigh |= (((valHigh >> shift) << s) & 0xFFL) << shift;
            resultLow |= (((valLow >> shift) << s) & 0xFFL) << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_ALL_TRUE(MStack stack, Instance instance, Machine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        if (valHigh == 0L && valLow == 0L) {
            stack.push(BitOps.TRUE);
        } else {
            stack.push(BitOps.FALSE);
        }
    }

    private static void V128_NOT(MStack stack, Instance instance, Machine.Operands operands) {
        // TODO: this is the only operation really implemented with the Vector API
        // this should be almost an in-place replace on the stack, should we measure performance
        // against push and pop?

        var offset = stack.size() - 2;
        var not = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset).not();
        var res = not.toArray();

        System.arraycopy(res, 0, stack.array(), offset, 2);
    }

    private static void V128_BITSELECT(MStack stack, Instance instance, Machine.Operands operands) {
        // https://github.com/tetratelabs/wazero/blob/58488880a334e8bda5be0d715a24d8ddeb34c725/internal/engine/interpreter/interpreter.go#L2378C7-L2384
        var cHi = stack.pop();
        var cLo = stack.pop();
        var x2Hi = stack.pop();
        var x2Lo = stack.pop();
        var x1Hi = stack.pop();
        var x1Lo = stack.pop();
        // v128.or(v128.and(v1, c), v128.and(v2, v128.not(c)))
        stack.push((x1Lo & cLo) | (x2Lo & (cLo ^ 0xFFFFFFFFFFFFFFFFL)));
        stack.push((x1Hi & cHi) | (x2Hi & (cHi ^ 0xFFFFFFFFFFFFFFFFL)));
    }

    private static void F32x4_MUL(MStack stack, Instance instance, Machine.Operands operands) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (Float.floatToIntBits(
                                            Float.intBitsToFloat(
                                                            (int)
                                                                    ((val1High >> shift)
                                                                            & 0xFFFFFFFFL))
                                                    * Float.intBitsToFloat(
                                                            (int)
                                                                    ((val2High >> shift)
                                                                            & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (Float.floatToIntBits(
                                            Float.intBitsToFloat(
                                                            (int)
                                                                    ((val1Low >> shift)
                                                                            & 0xFFFFFFFFL))
                                                    * Float.intBitsToFloat(
                                                            (int)
                                                                    ((val2Low >> shift)
                                                                            & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F32x4_ABS(MStack stack, Instance instance, Machine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        // https://github.com/tetratelabs/wazero/blob/58488880a334e8bda5be0d715a24d8ddeb34c725/internal/engine/interpreter/interpreter.go#L3168-L3169
        long resultLow = valLow & (1L << 31 | 1L << 63) ^ 0xFFFFFFFFFFFFFFFFL;
        long resultHigh = valHigh & (1L << 31 | 1L << 63) ^ 0xFFFFFFFFFFFFFFFFL;

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F32x4_MIN(MStack stack, Instance instance, Machine.Operands operands) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (((val1High >> shift) & 0xFFFFFFFFL) < ((val2High >> shift) & 0xFFFFFFFFL)
                                    ? ((val2High >> shift) & 0xFFFFFFFFL)
                                    : ((val1High >> shift) & 0xFFFFFFFFL))
                            << shift;
            resultLow |=
                    (((val1Low >> shift) & 0xFFFFFFFFL) < ((val2Low >> shift) & 0xFFFFFFFFL)
                                    ? ((val2Low >> shift) & 0xFFFFFFFFL)
                                    : ((val1Low >> shift) & 0xFFFFFFFFL))
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I32x4_TRUNC_SAT_F32x4_S(
            MStack stack, Instance instance, Machine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (OpcodeImpl.I32_TRUNC_SAT_F32_S(
                                            Float.intBitsToFloat(
                                                    (int) ((valHigh >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (OpcodeImpl.I32_TRUNC_SAT_F32_S(
                                            Float.intBitsToFloat(
                                                    (int) ((valLow >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F32x4_CONVERT_I32x4_U(
            MStack stack, Instance instance, Machine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_U(
                                                    (int) ((valHigh >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_U(
                                                    (int) ((valLow >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_SWIZZLE(MStack stack, Instance instance, Machine.Operands operands) {
        var idxHigh = stack.pop();
        var idxLow = stack.pop();
        var baseHigh = stack.pop();
        var baseLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 16; i++) {
            long id = 0;
            if (i < 8) {
                id = (idxLow >> (i * 8)) & 0xFFL;
            } else {
                id = (idxHigh >> ((i - 8) * 8)) & 0xFFL;
            }

            long base;
            if (id < 8) {
                base = (baseLow >> (id * 8)) & 0xFFL;
            } else if (id < 16) {
                base = (baseHigh >> (id * 8)) & 0xFFL;
            } else {
                base = 0x00L;
            }

            if (i < 8) {
                resultLow |= base << (i * 8);
            } else {
                resultHigh |= base << ((i - 8) * 8);
            }
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }
}
