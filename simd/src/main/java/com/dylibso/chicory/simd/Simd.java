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
        opcodesImpl.put(OpCode.I8x16_EQ, Simd::I8x16_EQ);
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
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();
        go on from here
    }

//  SPLAT 8 implementation
//        var ptr = readMemPtr(stack, operands);
//        var b = (long) instance.memory().read(ptr);
//        var val = Value.v128(b << 56 | b << 48 | b << 40 | b << 32 | b << 24 | b << 16 | b << 8 | b);
//        stack.push(val);
//        stack.push(val);
}
