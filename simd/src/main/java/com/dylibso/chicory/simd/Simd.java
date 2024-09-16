package com.dylibso.chicory.simd;

import static com.dylibso.chicory.runtime.InterpreterMachine.readMemPtr;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import java.util.HashMap;
import java.util.Map;

public class Simd {

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
        opcodesImpl.put(OpCode.I8x16_ALL_TRUE, Simd::I8x16_ALL_TRUE);
        opcodesImpl.put(OpCode.I8x16_SHL, Simd::I8x16_SHL);
        opcodesImpl.put(OpCode.F32x4_MUL, Simd::F32x4_MUL);
        opcodesImpl.put(OpCode.F32x4_ABS, Simd::F32x4_ABS);
        opcodesImpl.put(OpCode.F32x4_MIN, Simd::F32x4_MIN);
    }

    private static void V128_CONST(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        stack.push(Value.v128(operands.get(0)));
        stack.push(Value.v128(operands.get(1)));
    }

    private static void V128_LOAD(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var valHigh = instance.memory().readV128(ptr);
        var valLow = instance.memory().readV128(ptr + 8);
        stack.push(valHigh);
        stack.push(valLow);
    }

    private static void I8x16_EXTRACT_LANE_S(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();
        Value result;
        var laneIdx = operands.get(0);
        if (laneIdx < 8) {
            result = Value.i32(valLow.asLong() >> (laneIdx * 8) & 0xFF);
        } else {
            result = Value.i32(valHigh.asLong() >> (laneIdx * 8) & 0xFF);
        }
        stack.push(result);
    }

    private static void I8x16_EQ(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var val1High = stack.pop().asLong();
        var val1Low = stack.pop().asLong();
        var val2High = stack.pop().asLong();
        var val2Low = stack.pop().asLong();

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

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void I8x16_ADD(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var val1High = stack.pop().asLong();
        var val1Low = stack.pop().asLong();
        var val2High = stack.pop().asLong();
        var val2Low = stack.pop().asLong();

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

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void I8x16_SUB(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var val1High = stack.pop().asLong();
        var val1Low = stack.pop().asLong();
        var val2High = stack.pop().asLong();
        var val2Low = stack.pop().asLong();

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

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void I8x16_SHL(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var s = stack.pop().asInt();
        var valHigh = stack.pop().asLong();
        var valLow = stack.pop().asLong();

        long resultLow = 0L;
        long resultHigh = 0L;
        for (int i = 0; i < 8; i++) {
            var shift = i * 8L;
            resultHigh |= (((valHigh >> shift) << s) & 0xFFL) << shift;
            resultLow |= (((valLow >> shift) << s) & 0xFFL) << shift;
        }

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void I8x16_ALL_TRUE(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var valHigh = stack.pop().asLong();
        var valLow = stack.pop().asLong();

        if (valHigh == 0L && valLow == 0L) {
            stack.push(Value.i32(1));
        } else {
            stack.push(Value.i32(0));
        }
    }

    private static void V128_NOT(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var valHigh = stack.pop().asLong();
        var valLow = stack.pop().asLong();

        // With real Java 21 should be something similar to:
        // let's try this one as a real SIMD before sending the PR
        // LongVector.fromArray(LongVector.SPECIES_64, new long[] { valHigh, valLow }, 0).not();

        stack.push(Value.v128(valLow ^ 0xFFFFFFFFFFFFFFFFL));
        stack.push(Value.v128(valHigh ^ 0xFFFFFFFFFFFFFFFFL));
    }

    private static void V128_BITSELECT(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        // https://github.com/tetratelabs/wazero/blob/58488880a334e8bda5be0d715a24d8ddeb34c725/internal/engine/interpreter/interpreter.go#L2378C7-L2384
        var cHi = stack.pop().asLong();
        var cLo = stack.pop().asLong();
        var x2Hi = stack.pop().asLong();
        var x2Lo = stack.pop().asLong();
        var x1Hi = stack.pop().asLong();
        var x1Lo = stack.pop().asLong();
        // v128.or(v128.and(v1, c), v128.and(v2, v128.not(c)))
        stack.push(Value.v128((x1Lo & cLo) | (x2Lo & (cLo ^ 0xFFFFFFFFFFFFFFFFL))));
        stack.push(Value.v128((x1Hi & cHi) | (x2Hi & (cHi ^ 0xFFFFFFFFFFFFFFFFL))));
    }

    private static void F32x4_MUL(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var val1High = stack.pop().asLong();
        var val1Low = stack.pop().asLong();
        var val2High = stack.pop().asLong();
        var val2Low = stack.pop().asLong();

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

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void F32x4_ABS(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var valHigh = stack.pop().asLong();
        var valLow = stack.pop().asLong();

        // https://github.com/tetratelabs/wazero/blob/58488880a334e8bda5be0d715a24d8ddeb34c725/internal/engine/interpreter/interpreter.go#L3168-L3169
        long resultLow = valLow & (1L<<31 | 1L<<63) ^ 0xFFFFFFFFFFFFFFFFL;
        long resultHigh = valHigh & (1L<<31 | 1L<<63) ^ 0xFFFFFFFFFFFFFFFFL;

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }

    private static void F32x4_MIN(
            MStack stack, Instance instance, InterpreterMachine.Operands operands) {
        var val1High = stack.pop().asLong();
        var val1Low = stack.pop().asLong();
        var val2High = stack.pop().asLong();
        var val2Low = stack.pop().asLong();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |= (((val1High >> shift) & 0xFFFFFFFFL) < ((val2High >> shift) & 0xFFFFFFFFL) ? ((val2High >> shift) & 0xFFFFFFFFL) : ((val1High >> shift) & 0xFFFFFFFFL)) << shift;
            resultLow |= (((val1Low >> shift) & 0xFFFFFFFFL) < ((val2Low >> shift) & 0xFFFFFFFFL) ? ((val2Low >> shift) & 0xFFFFFFFFL) : ((val1Low >> shift) & 0xFFFFFFFFL)) << shift;
        }

        stack.push(Value.v128(resultLow));
        stack.push(Value.v128(resultHigh));
    }
}
