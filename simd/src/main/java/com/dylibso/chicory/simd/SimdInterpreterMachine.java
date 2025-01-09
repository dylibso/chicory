package com.dylibso.chicory.simd;

import com.dylibso.chicory.runtime.BitOps;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Function;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;

public final class SimdInterpreterMachine extends InterpreterMachine {

    public SimdInterpreterMachine(Instance instance) {
        super(instance);
    }

    @Override
    protected void evalDefault(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            Instruction instruction,
            Operands operands)
            throws ChicoryException {
        switch (instruction.opcode()) {
            case OpCode.V128_CONST:
                V128_CONST(stack, operands);
                break;
            case OpCode.V128_LOAD:
                V128_LOAD(stack, instance, operands);
                break;
            case OpCode.V128_LOAD32_ZERO:
                V128_LOAD32_ZERO(stack, instance, operands);
                break;
            case OpCode.V128_LOAD64_ZERO:
                V128_LOAD64_ZERO(stack, instance, operands);
                break;
            case OpCode.V128_STORE:
                V128_STORE(stack, instance, operands);
                break;
            case OpCode.V128_STORE8_LANE:
                STORE_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                instance.memory()
                                        .writeByte(
                                                ptr,
                                                v.reinterpretAsBytes()
                                                        .lane((int) operands.get(2))));
                break;
            case OpCode.V128_STORE16_LANE:
                STORE_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                instance.memory()
                                        .writeShort(
                                                ptr,
                                                v.reinterpretAsShorts()
                                                        .lane((int) operands.get(2))));
                break;
            case OpCode.V128_STORE32_LANE:
                STORE_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                instance.memory()
                                        .writeI32(
                                                ptr,
                                                v.reinterpretAsInts().lane((int) operands.get(2))));
                break;
            case OpCode.V128_STORE64_LANE:
                STORE_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                instance.memory()
                                        .writeLong(
                                                ptr,
                                                v.reinterpretAsLongs()
                                                        .lane((int) operands.get(2))));
                break;
            case OpCode.I8x16_EXTRACT_LANE_S:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v -> (long) v.reinterpretAsBytes().lane((int) operands.get(0)));
                break;
            case OpCode.I32x4_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v -> (long) v.reinterpretAsInts().lane((int) operands.get(0)));
                break;
            case OpCode.I64x2_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack, operands, v -> v.reinterpretAsLongs().lane((int) operands.get(0)));
                break;
            case OpCode.V128_NOT:
                V128_NOT(stack);
                break;
            case OpCode.V128_BITSELECT:
                V128_BITSELECT(stack);
                break;
            case OpCode.I8x16_EQ:
                I8x16_EQ(stack);
                break;
            case OpCode.I8x16_SUB:
                I8x16_SUB(stack);
                break;
            case OpCode.I8x16_SWIZZLE:
                I8x16_SWIZZLE(stack);
                break;
            case OpCode.I8x16_ALL_TRUE:
                I8x16_ALL_TRUE(stack);
                break;
            case OpCode.I8x16_SHL:
                I8x16_SHL(stack);
                break;
            case OpCode.I8x16_ADD:
                ADD(stack, LongVector::reinterpretAsBytes);
                break;
            case OpCode.I32x4_ADD:
                ADD(stack, LongVector::reinterpretAsInts);
                break;
            case OpCode.I64x2_ADD:
                ADD(stack, LongVector::reinterpretAsLongs);
                break;
            case OpCode.F32x4_MUL:
                F32x4_MUL(stack);
                break;
            case OpCode.F32x4_ABS:
                F32x4_ABS(stack);
                break;
            case OpCode.F32x4_MIN:
                F32x4_MIN(stack);
                break;
            case OpCode.I32x4_TRUNC_SAT_F32X4_S:
                I32x4_TRUNC_SAT_F32x4_S(stack);
                break;
            case OpCode.F32x4_CONVERT_I32x4_U:
                F32x4_CONVERT_I32x4_U(stack);
                break;
            default:
                super.evalDefault(stack, instance, callStack, instruction, operands);
                break;
        }
    }

    private static void V128_CONST(MStack stack, Operands operands) {
        stack.push(operands.get(0));
        stack.push(operands.get(1));
    }

    private static void V128_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var valHigh = instance.memory().readLong(ptr);
        var valLow = instance.memory().readLong(ptr + 8);
        stack.push(valHigh);
        stack.push(valLow);
    }

    private static void V128_LOAD32_ZERO(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readInt(ptr);
        var vals = Value.i32ToVec(new long[] {val, 0, 0, 0});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD64_ZERO(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readLong(ptr);
        var vals = Value.i64ToVec(new long[] {val, 0});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_STORE(MStack stack, Instance instance, Operands operands) {
        var valHigh = stack.pop();
        var valLow = stack.pop();
        var offset = operands.get(1);
        var i = stack.pop();
        // to let the bounds check kick in appropriately
        var ptr = (i >= 0) ? (int) (offset + i) : (int) i;

        instance.memory().writeLong(ptr, valLow);
        instance.memory().writeLong(ptr + 8, valHigh);
    }

    private static void STORE_LANE(
            MStack stack, Operands operands, BiConsumer<LongVector, Integer> store) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var offset = operands.get(1);
        var i = stack.pop();
        // to let the bounds check kick in appropriately
        var ptr = (i >= 0) ? (int) (offset + i) : (int) i;

        var result = LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0);

        store.accept(result, ptr);
    }

    private static void EXTRACT_LANE(
            MStack stack, Operands operands, Function<LongVector, Long> extract) {
        var offset = stack.size() - 2;
        var result =
                extract.apply(LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset));

        // consume one element
        stack.pop();
        stack.array()[stack.size() - 1] = result;
    }

    private static void I8x16_EQ(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v1.eq(v2).toVector().reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void ADD(MStack stack, Function<LongVector, Vector> reinterpret) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                reinterpret.apply(
                        LongVector.fromArray(
                                LongVector.SPECIES_128, new long[] {v1Low, v1High}, offset));
        var v2 =
                reinterpret.apply(
                        LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset));

        var result = v1.add(v2).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SUB(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, offset)
                        .reinterpretAsBytes();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v2.sub(v1).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SHL(MStack stack) {
        var s = stack.pop();

        var offset = stack.size() - 2;

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v.lanewise(VectorOperators.LSHL, s).reinterpretAsLongs().toArray();
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_ALL_TRUE(MStack stack) {
        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), stack.size() - 2)
                        .reinterpretAsBytes();

        // Compare the vector against zero to create a mask of non-zero elements
        if (v.compare(VectorOperators.NE, 0).allTrue()) {
            stack.push(BitOps.TRUE);
        } else {
            stack.push(BitOps.FALSE);
        }
    }

    private static void V128_NOT(MStack stack) {
        var offset = stack.size() - 2;
        var not = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset).not();
        var res = not.toArray();

        System.arraycopy(res, 0, stack.array(), offset, 2);
    }

    private static void V128_BITSELECT(MStack stack) {
        var cHi = stack.pop();
        var cLo = stack.pop();
        var x1Hi = stack.pop();
        var x1Lo = stack.pop();

        var m = LongVector.fromArray(LongVector.SPECIES_128, new long[] {cLo, cHi}, 0);
        var v1 = LongVector.fromArray(LongVector.SPECIES_128, new long[] {x1Lo, x1Hi}, 0);
        var v2 = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), stack.size() - 2);

        var result = v1.bitwiseBlend(v2, m).toArray();

        System.arraycopy(result, 0, stack.array(), stack.size() - 2, 2);
    }

    private static void F32x4_MUL(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsFloats();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats();

        var result = v1.mul(v2).reinterpretAsLongs().toArray();
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void F32x4_ABS(MStack stack) {
        var offset = stack.size() - 2;

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats();

        var result = v.abs().reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void F32x4_MIN(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsFloats();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats();

        var result = v1.min(v2).reinterpretAsLongs().toArray();
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I32x4_TRUNC_SAT_F32x4_S(MStack stack) {
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

    private static void F32x4_CONVERT_I32x4_U(MStack stack) {
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

    private static void I8x16_SWIZZLE(MStack stack) {
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
