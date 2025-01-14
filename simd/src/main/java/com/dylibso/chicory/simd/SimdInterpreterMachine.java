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
import java.util.Arrays;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
            case OpCode.V128_LOAD8_LANE:
                LOAD_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                v.reinterpretAsBytes()
                                        .withLane(
                                                (int) operands.get(2), instance.memory().read(ptr))
                                        .reinterpretAsLongs());
                break;
            case OpCode.V128_LOAD16_LANE:
                LOAD_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                v.reinterpretAsShorts()
                                        .withLane(
                                                (int) operands.get(2),
                                                instance.memory().readShort(ptr))
                                        .reinterpretAsLongs());
                break;
            case OpCode.V128_LOAD32_LANE:
                LOAD_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                v.reinterpretAsInts()
                                        .withLane(
                                                (int) operands.get(2),
                                                instance.memory().readInt(ptr))
                                        .reinterpretAsLongs());
                break;
            case OpCode.V128_LOAD64_LANE:
                LOAD_LANE(
                        stack,
                        operands,
                        (v, ptr) ->
                                v.withLane((int) operands.get(2), instance.memory().readLong(ptr)));
                break;
            case OpCode.V128_LOAD8x8_S:
                V128_LOAD8x8_S(stack, instance, operands);
                break;
            case OpCode.V128_LOAD8x8_U:
                V128_LOAD8x8_U(stack, instance, operands);
                break;
            case OpCode.V128_LOAD16x4_S:
                V128_LOAD16x4_S(stack, instance, operands);
                break;
            case OpCode.V128_LOAD16x4_U:
                V128_LOAD16x4_U(stack, instance, operands);
                break;
            case OpCode.V128_LOAD32x2_S:
                V128_LOAD32x2_S(stack, instance, operands);
                break;
            case OpCode.V128_LOAD32x2_U:
                V128_LOAD32x2_U(stack, instance, operands);
                break;
            case OpCode.V128_STORE:
                V128_STORE(stack, instance, operands);
                break;
            case OpCode.V128_LOAD8_SPLAT:
                V128_LOAD8_SPLAT(stack, instance, operands);
                break;
            case OpCode.V128_LOAD16_SPLAT:
                V128_LOAD16_SPLAT(stack, instance, operands);
                break;
            case OpCode.V128_LOAD32_SPLAT:
                V128_LOAD32_SPLAT(stack, instance, operands);
                break;
            case OpCode.V128_LOAD64_SPLAT:
                V128_LOAD64_SPLAT(stack, instance, operands);
                break;
            case OpCode.I8x16_SHUFFLE:
                I8x16_SHUFFLE(stack, operands);
                break;
            case OpCode.I8x16_SPLAT:
                I8x16_SPLAT(stack);
                break;
            case OpCode.I16x8_SPLAT:
                I16x8_SPLAT(stack);
                break;
            case OpCode.I32x4_SPLAT:
                I32x4_SPLAT(stack);
                break;
            case OpCode.F32x4_SPLAT:
                F32x4_SPLAT(stack);
                break;
            case OpCode.I64x2_SPLAT:
                I64x2_SPLAT(stack);
                break;
            case OpCode.F64x2_SPLAT:
                F64x2_SPLAT(stack);
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
            case OpCode.I8x16_REPLACE_LANE:
                REPLACE_LANE(
                        stack,
                        (v, val) ->
                                v.reinterpretAsBytes()
                                        .withLane((int) operands.get(0), val.byteValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I16x8_REPLACE_LANE:
                REPLACE_LANE(
                        stack,
                        (v, val) ->
                                v.reinterpretAsShorts()
                                        .withLane((int) operands.get(0), val.shortValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I32x4_REPLACE_LANE:
                REPLACE_LANE(
                        stack,
                        (v, val) ->
                                v.reinterpretAsInts()
                                        .withLane((int) operands.get(0), val.intValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.F32x4_REPLACE_LANE:
                REPLACE_LANE(
                        stack,
                        (v, val) ->
                                v.reinterpretAsFloats()
                                        .withLane((int) operands.get(0), Value.longToFloat(val))
                                        .reinterpretAsLongs());
                break;
            case OpCode.I64x2_REPLACE_LANE:
                REPLACE_LANE(stack, (v, val) -> v.withLane((int) operands.get(0), val));
                break;
            case OpCode.F64x2_REPLACE_LANE:
                REPLACE_LANE(
                        stack,
                        (v, val) ->
                                v.reinterpretAsDoubles()
                                        .withLane((int) operands.get(0), Value.longToDouble(val))
                                        .reinterpretAsLongs());
                break;
            case OpCode.I8x16_EXTRACT_LANE_U:
                I8x16_EXTRACT_LANE_U(stack, operands);
                break;
            case OpCode.I16x8_EXTRACT_LANE_U:
                I16x8_EXTRACT_LANE_U(stack, operands);
                break;
            case OpCode.I8x16_EXTRACT_LANE_S:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v -> (long) v.reinterpretAsBytes().lane((int) operands.get(0)));
                break;
            case OpCode.I16x8_EXTRACT_LANE_S:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v -> (long) v.reinterpretAsShorts().lane((int) operands.get(0)));
                break;
            case OpCode.I32x4_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v -> (long) v.reinterpretAsInts().lane((int) operands.get(0)));
                break;
            case OpCode.F32x4_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v ->
                                Value.floatToLong(
                                        v.reinterpretAsFloats().lane((int) operands.get(0))));
                break;
            case OpCode.I64x2_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack, operands, v -> v.reinterpretAsLongs().lane((int) operands.get(0)));
                break;
            case OpCode.F64x2_EXTRACT_LANE:
                EXTRACT_LANE(
                        stack,
                        operands,
                        v ->
                                Value.doubleToLong(
                                        v.reinterpretAsDoubles().lane((int) operands.get(0))));
                break;
            case OpCode.V128_NOT:
                V128_NOT(stack);
                break;
            case OpCode.V128_AND:
                V128_BINOP(stack, (v1, v2) -> v1.and(v2));
                break;
            case OpCode.V128_ANDNOT:
                V128_BINOP(stack, (v1, v2) -> v1.not().and(v2));
                break;
            case OpCode.V128_OR:
                V128_BINOP(stack, (v1, v2) -> v1.or(v2));
                break;
            case OpCode.V128_XOR:
                V128_BINOP(stack, (v1, v2) -> v1.and(v2.not()).or(v1.not().and(v2)));
                break;
            case OpCode.V128_BITSELECT:
                V128_BITSELECT(stack);
                break;
            case OpCode.V128_ANY_TRUE:
                V128_ANY_TRUE(stack);
                break;
            case OpCode.I8x16_EQ:
                BINOP(stack, LongVector::reinterpretAsBytes, (v1, v2) -> v1.eq(v2).toVector());
                break;
            case OpCode.I16x8_EQ:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v1.eq(v2).toVector());
                break;
            case OpCode.I32x4_EQ:
                BINOP(stack, LongVector::reinterpretAsInts, (v1, v2) -> v1.eq(v2).toVector());
                break;
            case OpCode.F32x4_EQ:
                F32x4_EQ(stack);
                break;
            case OpCode.F64x2_EQ:
                BINOP(stack, LongVector::reinterpretAsDoubles, (v1, v2) -> v1.eq(v2).toVector());
                break;
            case OpCode.I8x16_SUB:
                I8x16_SUB(stack);
                break;
            case OpCode.I8x16_SWIZZLE:
                I8x16_SWIZZLE(stack);
                break;
            case OpCode.I8x16_ALL_TRUE:
                BOOL_OP(
                        stack,
                        v -> v.reinterpretAsBytes().compare(VectorOperators.NE, 0).allTrue());
                break;
            case OpCode.I16x8_ALL_TRUE:
                BOOL_OP(
                        stack,
                        v -> v.reinterpretAsShorts().compare(VectorOperators.NE, 0).allTrue());
                break;
            case OpCode.I32x4_ALL_TRUE:
                BOOL_OP(stack, v -> v.reinterpretAsInts().compare(VectorOperators.NE, 0).allTrue());
                break;
            case OpCode.I64x2_ALL_TRUE:
                BOOL_OP(stack, v -> v.compare(VectorOperators.NE, 0).allTrue());
                break;
            case OpCode.I8x16_BITMASK:
                BITMASK(stack, v -> v.reinterpretAsBytes().toLongArray());
                break;
            case OpCode.I16x8_BITMASK:
                BITMASK(stack, v -> v.reinterpretAsShorts().toLongArray());
                break;
            case OpCode.I32x4_BITMASK:
                BITMASK(stack, v -> v.reinterpretAsInts().toLongArray());
                break;
            case OpCode.I64x2_BITMASK:
                BITMASK(stack, v -> v.toLongArray());
                break;
            case OpCode.I8x16_SHL:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsBytes()
                                        .lanewise(VectorOperators.LSHL, s.byteValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I8x16_SHR_U:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsBytes()
                                        .lanewise(VectorOperators.LSHR, s.byteValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I8x16_SHR_S:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsBytes()
                                        .lanewise(VectorOperators.ASHR, s.byteValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I8x16_ADD:
                BINOP(stack, LongVector::reinterpretAsBytes, (v1, v2) -> v1.add(v2));
                break;
            case OpCode.I8x16_ADD_SAT_S:
                I8x16_ADD_SAT_S(stack);
                break;
            case OpCode.I8x16_ADD_SAT_U:
                I8x16_ADD_SAT_U(stack);
                break;
            case OpCode.I8x16_SUB_SAT_U:
                I8x16_SUB_SAT_U(stack);
                break;
            case OpCode.I8x16_SUB_SAT_S:
                I8x16_SUB_SAT_S(stack);
                break;
            case OpCode.I8x16_MIN_S:
                BINOP(stack, LongVector::reinterpretAsBytes, (v1, v2) -> v1.min(v2));
                break;
            case OpCode.I8x16_MAX_S:
                BINOP(stack, LongVector::reinterpretAsBytes, (v1, v2) -> v1.max(v2));
                break;
            case OpCode.I8x16_MAX_U:
                I8x16(
                        stack,
                        (a, b) -> (long) Math.max(Byte.toUnsignedInt(a), Byte.toUnsignedInt(b)));
                break;
            case OpCode.I8x16_MIN_U:
                I8x16(
                        stack,
                        (a, b) -> (long) Math.min(Byte.toUnsignedInt(a), Byte.toUnsignedInt(b)));
                break;
            case OpCode.I8x16_AVGR_U:
                I8x16(
                        stack,
                        (a, b) -> (long) ((Byte.toUnsignedInt(a) + Byte.toUnsignedInt(b) + 1) / 2));
                break;
            case OpCode.I8x16_ABS:
                UNARY(stack, LongVector::reinterpretAsBytes, Vector::abs);
                break;
            case OpCode.I8x16_NEG:
                UNARY(stack, LongVector::reinterpretAsBytes, Vector::neg);
                break;
            case OpCode.I8x16_NE:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v1.eq(v2).not().toVector());
                break;
            case OpCode.I8x16_LT_S:
                BINOP(stack, LongVector::reinterpretAsBytes, (v1, v2) -> v2.lt(v1).toVector());
                break;
            case OpCode.I8x16_LT_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_LT, v1).toVector());
                break;
            case OpCode.I8x16_LE_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.LE, v1).toVector());
                break;
            case OpCode.I8x16_LE_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_LE, v1).toVector());
                break;
            case OpCode.I8x16_GT_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.GT, v1).toVector());
                break;
            case OpCode.I8x16_GT_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_GT, v1).toVector());
                break;
            case OpCode.I8x16_GE_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.GE, v1).toVector());
                break;
            case OpCode.I8x16_GE_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsBytes,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_GE, v1).toVector());
                break;
            case OpCode.I8x16_POPCNT:
                UNARY(
                        stack,
                        LongVector::reinterpretAsBytes,
                        v -> v.lanewise(VectorOperators.BIT_COUNT));
                break;
            case OpCode.I16x8_NEG:
                UNARY(stack, LongVector::reinterpretAsShorts, Vector::neg);
                break;
            case OpCode.I16x8_NE:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v1.eq(v2).not().toVector());
                break;
            case OpCode.I16x8_LT_S:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v2.lt(v1).toVector());
                break;
            case OpCode.I16x8_LT_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_LT, v1).toVector());
                break;
            case OpCode.I16x8_LE_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.LE, v1).toVector());
                break;
            case OpCode.I16x8_LE_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_LE, v1).toVector());
                break;
            case OpCode.I16x8_GT_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.GT, v1).toVector());
                break;
            case OpCode.I16x8_GT_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_GT, v1).toVector());
                break;
            case OpCode.I16x8_GE_S:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.GE, v1).toVector());
                break;
            case OpCode.I16x8_GE_U:
                BINOP(
                        stack,
                        LongVector::reinterpretAsShorts,
                        (v1, v2) -> v2.compare(VectorOperators.UNSIGNED_GE, v1).toVector());
                break;
            case OpCode.I16x8_MIN_S:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v1.min(v2));
                break;
            case OpCode.I16x8_MAX_S:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v1.max(v2));
                break;
            case OpCode.I16x8_MAX_U:
                I16x8(
                        stack,
                        (a, b) -> (long) Math.max(Short.toUnsignedInt(a), Short.toUnsignedInt(b)));
                break;
            case OpCode.I16x8_MIN_U:
                I16x8(
                        stack,
                        (a, b) -> (long) Math.min(Short.toUnsignedInt(a), Short.toUnsignedInt(b)));
                break;
            case OpCode.I16x8_AVGR_U:
                I16x8(
                        stack,
                        (a, b) ->
                                (long) ((Short.toUnsignedInt(a) + Short.toUnsignedInt(b) + 1) / 2));
                break;
            case OpCode.I16x8_ADD:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v1.add(v2));
                break;
            case OpCode.I16x8_ADD_SAT_S:
                I16x8_ADD_SAT_S(stack);
                break;
            case OpCode.I16x8_ADD_SAT_U:
                I16x8_ADD_SAT_U(stack);
                break;
            case OpCode.I16x8_SUB_SAT_U:
                I16x8_SUB_SAT_U(stack);
                break;
            case OpCode.I16x8_SUB_SAT_S:
                I16x8_SUB_SAT_S(stack);
                break;
            case OpCode.I16x8_SUB:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v2.sub(v1));
                break;
            case OpCode.I16x8_ABS:
                UNARY(stack, LongVector::reinterpretAsShorts, Vector::abs);
                break;
            case OpCode.F32x4_DIV:
                BINOP(stack, LongVector::reinterpretAsFloats, (v1, v2) -> v2.div(v1));
                break;
            case OpCode.I64x2_MUL:
                BINOP(stack, LongVector::reinterpretAsLongs, (v1, v2) -> v1.mul(v2));
                break;
            case OpCode.I64x2_SUB:
                BINOP(stack, LongVector::reinterpretAsLongs, (v1, v2) -> v2.sub(v1));
                break;
            case OpCode.F64x2_ADD:
                BINOP(stack, LongVector::reinterpretAsLongs, (v1, v2) -> v1.add(v2));
                break;
            case OpCode.F64x2_MUL:
                BINOP(stack, LongVector::reinterpretAsDoubles, (v1, v2) -> v1.mul(v2));
                break;
            case OpCode.F64x2_SUB:
                BINOP(stack, LongVector::reinterpretAsDoubles, (v1, v2) -> v2.sub(v1));
                break;
            case OpCode.I16x8_SHL:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsShorts()
                                        .lanewise(VectorOperators.LSHL, s.shortValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I16x8_SHR_U:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsShorts()
                                        .lanewise(VectorOperators.LSHR, s.shortValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I16x8_SHR_S:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsShorts()
                                        .lanewise(VectorOperators.ASHR, s.shortValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I32x4_SHL:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsInts()
                                        .lanewise(VectorOperators.LSHL, s.intValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I32x4_SHR_U:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsInts()
                                        .lanewise(VectorOperators.LSHR, s.intValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I32x4_SHR_S:
                SH(
                        stack,
                        (v, s) ->
                                v.reinterpretAsInts()
                                        .lanewise(VectorOperators.ASHR, s.intValue())
                                        .reinterpretAsLongs());
                break;
            case OpCode.I16x8_MUL:
                BINOP(stack, LongVector::reinterpretAsShorts, (v1, v2) -> v2.mul(v1));
                break;
            case OpCode.I32x4_ADD:
                BINOP(stack, LongVector::reinterpretAsInts, (v1, v2) -> v2.add(v1));
                break;
            case OpCode.I32x4_SUB:
                BINOP(stack, LongVector::reinterpretAsInts, (v1, v2) -> v2.sub(v1));
                break;
            case OpCode.I32x4_MUL:
                BINOP(stack, LongVector::reinterpretAsInts, (v1, v2) -> v2.mul(v1));
                break;
            case OpCode.I64x2_SHL:
                SH(stack, (v, s) -> v.lanewise(VectorOperators.LSHL, s));
                break;
            case OpCode.I64x2_SHR_U:
                SH(stack, (v, s) -> v.lanewise(VectorOperators.LSHR, s));
                break;
            case OpCode.I64x2_SHR_S:
                SH(stack, (v, s) -> v.lanewise(VectorOperators.ASHR, s));
                break;
            case OpCode.I64x2_ADD:
                BINOP(stack, LongVector::reinterpretAsLongs, (v1, v2) -> v2.add(v1));
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
            case OpCode.F32x4_CONVERT_I32x4_S:
                F32x4_CONVERT_I32x4_S(stack);
                break;
            case OpCode.F32x4_CONVERT_I32x4_U:
                F32x4_CONVERT_I32x4_U(stack);
                break;
            case OpCode.F64x2_PROMOTE_LOW_F32x4:
            case OpCode.F64x2_CONVERT_LOW_I32x4_S:
            case OpCode.F64x2_CONVERT_LOW_I32x4_U:
                F64x2_PROMOTE_LOW_F32x4(stack);
                break;
            case OpCode.F32x4_DEMOTE_LOW_F64x2_ZERO:
                F32x4_DEMOTE_LOW_F64x2_ZERO(stack);
                break;
            case OpCode.I8x16_NARROW_I16x8_S:
                I8x16_NARROW_I16x8(stack, SimdInterpreterMachine::narrowS);
                break;
            case OpCode.I8x16_NARROW_I16x8_U:
                I8x16_NARROW_I16x8(stack, SimdInterpreterMachine::narrowU);
                break;
            case OpCode.I16x8_EXTADD_PAIRWISE_I8x16_S:
                I16x8_EXTADD_PAIRWISE_I8x16_S(stack);
                break;
            case OpCode.I16x8_EXTADD_PAIRWISE_I8x16_U:
                I16x8_EXTADD_PAIRWISE_I8x16_U(stack);
                break;
            case OpCode.I16x8_EXTMUL_LOW_I8x16_S:
                I16x8_EXTMUL_LOW_I8x16_S(stack);
                break;
            case OpCode.I16x8_EXTMUL_HIGH_I8x16_S:
                I16x8_EXTMUL_HIGH_I8x16_S(stack);
                break;
            case OpCode.I16x8_EXTMUL_LOW_I8x16_U:
                I16x8_EXTMUL_LOW_I8x16_U(stack);
                break;
            case OpCode.I16x8_EXTMUL_HIGH_I8x16_U:
                I16x8_EXTMUL_HIGH_I8x16_U(stack);
                break;
            case OpCode.I16x8_Q15MULR_SAT_S:
                I16x8_Q15MULR_SAT_S(stack);
                break;
            case OpCode.I16x8_NARROW_I32x4_S:
                I16x8_NARROW_I32x4(stack, SimdInterpreterMachine::narrowS);
                break;
            case OpCode.I16x8_NARROW_I32x4_U:
                I16x8_NARROW_I32x4(stack, SimdInterpreterMachine::narrowU);
                break;
            case OpCode.I16x8_EXTEND_LOW_I8x16_S:
                I16x8_EXTEND_LOW_I8x16_S(stack);
                break;
            case OpCode.I16x8_EXTEND_HIGH_I8x16_S:
                I16x8_EXTEND_HIGH_I8x16_S(stack);
                break;
            case OpCode.I16x8_EXTEND_LOW_I8x16_U:
                I16x8_EXTEND_LOW_I8x16_U(stack);
                break;
            case OpCode.I16x8_EXTEND_HIGH_I8x16_U:
                I16x8_EXTEND_HIGH_I8x16_U(stack);
                break;
            case OpCode.I32x4_EXTEND_LOW_I16x8_S:
                I32x4_EXTEND_LOW_I16x8_S(stack);
                break;
            case OpCode.I32x4_EXTEND_HIGH_I16x8_S:
                I32x4_EXTEND_HIGH_I16x8_S(stack);
                break;
            case OpCode.I32x4_EXTEND_LOW_I16x8_U:
                I32x4_EXTEND_LOW_I16x8_U(stack);
                break;
            case OpCode.I32x4_EXTEND_HIGH_I16x8_U:
                I32x4_EXTEND_HIGH_I16x8_U(stack);
                break;
            case OpCode.I64x2_EXTEND_LOW_I32x4_S:
                I64x2_EXTEND_LOW_I32x4_S(stack);
                break;
            case OpCode.I64x2_EXTEND_HIGH_I32x4_S:
                I64x2_EXTEND_HIGH_I32x4_S(stack);
                break;
            case OpCode.I64x2_EXTEND_LOW_I32x4_U:
                I64x2_EXTEND_LOW_I32x4_U(stack);
                break;
            case OpCode.I64x2_EXTEND_HIGH_I32x4_U:
                I64x2_EXTEND_HIGH_I32x4_U(stack);
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

    private static void V128_LOAD8_SPLAT(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var value = instance.memory().read(ptr);
        var bytes = new long[16];
        Arrays.fill(bytes, value);
        var vals = Value.i8ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD16_SPLAT(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var value = instance.memory().readShort(ptr);
        var shorts = new long[8];
        Arrays.fill(shorts, value);
        var vals = Value.i16ToVec(shorts);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD32_SPLAT(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var value = instance.memory().readInt(ptr);
        var ints = new long[4];
        Arrays.fill(ints, value);
        var vals = Value.i32ToVec(ints);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD64_SPLAT(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var value = instance.memory().readLong(ptr);
        stack.push(value);
        stack.push(value);
    }

    private static void V128_LOAD8x8_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = instance.memory().read(ptr + i);
        }
        var vals = Value.i16ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD8x8_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = Byte.toUnsignedLong(instance.memory().read(ptr + i));
        }
        var vals = Value.i16ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD16x4_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = instance.memory().readShort(ptr + (i * 2));
        }
        var vals = Value.i32ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD16x4_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = Short.toUnsignedLong(instance.memory().readShort(ptr + (i * 2)));
        }
        var vals = Value.i32ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD32x2_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[8];
        for (int i = 0; i < 2; i++) {
            bytes[i] = instance.memory().readInt(ptr + (i * 4));
        }
        var vals = Value.i64ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void V128_LOAD32x2_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var bytes = new long[8];
        for (int i = 0; i < 2; i++) {
            bytes[i] = Integer.toUnsignedLong(instance.memory().readInt(ptr + (i * 4)));
        }
        var vals = Value.i64ToVec(bytes);
        for (var v : vals) {
            stack.push(v);
        }
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

    private static void LOAD_LANE(
            MStack stack, Operands operands, BiFunction<LongVector, Integer, LongVector> loadLane) {
        var valHigh = stack.pop();
        var valLow = stack.pop();
        var ptr = readMemPtr(stack, operands);

        var result =
                loadLane.apply(
                                LongVector.fromArray(
                                        LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0),
                                ptr)
                        .toArray();

        for (var v : result) {
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

    private static void I8x16_SHUFFLE(MStack stack, Operands operands) {
        var v2High = stack.pop();
        var v2Low = stack.pop();
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var select =
                LongVector.fromArray(
                                LongVector.SPECIES_128,
                                new long[] {operands.get(0), operands.get(1)},
                                0)
                        .reinterpretAsBytes()
                        .toArray();

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v2Low, v2High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var result = new byte[16];
        for (int i = 0; i < 16; i++) {
            var s = select[i];
            if (s >= 16) {
                result[i] = v2[s - 16];
            } else {
                result[i] = v1[s];
            }
        }

        var res = Value.bytesToVec(result);
        for (var v : res) {
            stack.push(v);
        }
    }

    private static void I8x16_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals =
                Value.i8ToVec(
                        new long[] {
                            val, val, val, val, val, val, val, val, val, val, val, val, val, val,
                            val, val
                        });
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void I16x8_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals = Value.i16ToVec(new long[] {val, val, val, val, val, val, val, val});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void I32x4_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals = Value.i32ToVec(new long[] {val, val, val, val});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void F32x4_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals = Value.f32ToVec(new long[] {val, val, val, val});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void I64x2_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals = Value.i64ToVec(new long[] {val, val});
        for (var v : vals) {
            stack.push(v);
        }
    }

    private static void F64x2_SPLAT(MStack stack) {
        var val = stack.pop();
        var vals = Value.f64ToVec(new long[] {val, val});
        for (var v : vals) {
            stack.push(v);
        }
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

    private static void REPLACE_LANE(
            MStack stack, BiFunction<LongVector, Long, LongVector> replace) {
        var val = stack.pop();
        var offset = stack.size() - 2;

        var result =
                replace.apply(
                                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset),
                                val)
                        .toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_EXTRACT_LANE_U(MStack stack, Operands operands) {
        var offset = stack.size() - 2;
        var result =
                Byte.toUnsignedLong(
                        LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                                .reinterpretAsBytes()
                                .lane((int) operands.get(0)));

        // consume one element
        stack.pop();
        stack.array()[stack.size() - 1] = result;
    }

    private static void I16x8_EXTRACT_LANE_U(MStack stack, Operands operands) {
        var offset = stack.size() - 2;
        var result =
                Short.toUnsignedLong(
                        LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                                .reinterpretAsShorts()
                                .lane((int) operands.get(0)));

        // consume one element
        stack.pop();
        stack.array()[stack.size() - 1] = result;
    }

    public static byte addSatU(byte a, byte b) {
        int result = Byte.toUnsignedInt(a) + Byte.toUnsignedInt(b);
        if (result >= 0xFF) {
            return (byte) 0xFF;
        } else if (result < 0) {
            return 0;
        } else {
            return (byte) result;
        }
    }

    public static long addSatU(short a, Short b) {
        int result = Short.toUnsignedInt(a) + Short.toUnsignedInt(b);
        if (result >= 0xFFFF) {
            return 0xFFFF;
        } else {
            return result;
        }
    }

    public static byte subSatS(byte a, byte b) {
        int result = a - b;
        if (result > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        } else if (result < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        } else {
            return (byte) result;
        }
    }

    public static long subSatS(short a, short b) {
        int result = a - b;
        if (result > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        } else if (result < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        } else {
            return result;
        }
    }

    public static byte subSatU(byte a, byte b) {
        int result = Byte.toUnsignedInt(a) - Byte.toUnsignedInt(b);
        if (result < 0) {
            return 0;
        } else {
            return (byte) result;
        }
    }

    public static short subSatU(short a, short b) {
        int result = Short.toUnsignedInt(a) - Short.toUnsignedInt(b);
        if (result < 0) {
            return 0;
        } else {
            return (short) result;
        }
    }

    private static void I16x8_SUB_SAT_U(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var result =
                Value.i16ToVec(
                        new long[] {
                            subSatU(v2[0], v1[0]),
                            subSatU(v2[1], v1[1]),
                            subSatU(v2[2], v1[2]),
                            subSatU(v2[3], v1[3]),
                            subSatU(v2[4], v1[4]),
                            subSatU(v2[5], v1[5]),
                            subSatU(v2[6], v1[6]),
                            subSatU(v2[7], v1[7])
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_SUB_SAT_S(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var result =
                Value.i16ToVec(
                        new long[] {
                            subSatS(v2[0], v1[0]),
                            subSatS(v2[1], v1[1]),
                            subSatS(v2[2], v1[2]),
                            subSatS(v2[3], v1[3]),
                            subSatS(v2[4], v1[4]),
                            subSatS(v2[5], v1[5]),
                            subSatS(v2[6], v1[6]),
                            subSatS(v2[7], v1[7])
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SUB_SAT_U(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var result =
                Value.i8ToVec(
                        new long[] {
                            subSatU(v2[0], v1[0]),
                            subSatU(v2[1], v1[1]),
                            subSatU(v2[2], v1[2]),
                            subSatU(v2[3], v1[3]),
                            subSatU(v2[4], v1[4]),
                            subSatU(v2[5], v1[5]),
                            subSatU(v2[6], v1[6]),
                            subSatU(v2[7], v1[7]),
                            subSatU(v2[8], v1[8]),
                            subSatU(v2[9], v1[9]),
                            subSatU(v2[10], v1[10]),
                            subSatU(v2[11], v1[11]),
                            subSatU(v2[12], v1[12]),
                            subSatU(v2[13], v1[13]),
                            subSatU(v2[14], v1[14]),
                            subSatU(v2[15], v1[15]),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SUB_SAT_S(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var result =
                Value.i8ToVec(
                        new long[] {
                            subSatS(v2[0], v1[0]),
                            subSatS(v2[1], v1[1]),
                            subSatS(v2[2], v1[2]),
                            subSatS(v2[3], v1[3]),
                            subSatS(v2[4], v1[4]),
                            subSatS(v2[5], v1[5]),
                            subSatS(v2[6], v1[6]),
                            subSatS(v2[7], v1[7]),
                            subSatS(v2[8], v1[8]),
                            subSatS(v2[9], v1[9]),
                            subSatS(v2[10], v1[10]),
                            subSatS(v2[11], v1[11]),
                            subSatS(v2[12], v1[12]),
                            subSatS(v2[13], v1[13]),
                            subSatS(v2[14], v1[14]),
                            subSatS(v2[15], v1[15]),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_ADD_SAT_S(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var result =
                Value.i8ToVec(
                        new long[] {
                            narrowS((short) (v1[0] + v2[0])),
                            narrowS((short) (v1[1] + v2[1])),
                            narrowS((short) (v1[2] + v2[2])),
                            narrowS((short) (v1[3] + v2[3])),
                            narrowS((short) (v1[4] + v2[4])),
                            narrowS((short) (v1[5] + v2[5])),
                            narrowS((short) (v1[6] + v2[6])),
                            narrowS((short) (v1[7] + v2[7])),
                            narrowS((short) (v1[8] + v2[8])),
                            narrowS((short) (v1[9] + v2[9])),
                            narrowS((short) (v1[10] + v2[10])),
                            narrowS((short) (v1[11] + v2[11])),
                            narrowS((short) (v1[12] + v2[12])),
                            narrowS((short) (v1[13] + v2[13])),
                            narrowS((short) (v1[14] + v2[14])),
                            narrowS((short) (v1[15] + v2[15])),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_ADD_SAT_U(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var result =
                Value.i8ToVec(
                        new long[] {
                            addSatU(v1[0], v2[0]),
                            addSatU(v1[1], v2[1]),
                            addSatU(v1[2], v2[2]),
                            addSatU(v1[3], v2[3]),
                            addSatU(v1[4], v2[4]),
                            addSatU(v1[5], v2[5]),
                            addSatU(v1[6], v2[6]),
                            addSatU(v1[7], v2[7]),
                            addSatU(v1[8], v2[8]),
                            addSatU(v1[9], v2[9]),
                            addSatU(v1[10], v2[10]),
                            addSatU(v1[11], v2[11]),
                            addSatU(v1[12], v2[12]),
                            addSatU(v1[13], v2[13]),
                            addSatU(v1[14], v2[14]),
                            addSatU(v1[15], v2[15]),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_ADD_SAT_S(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var result =
                Value.i16ToVec(
                        new long[] {
                            narrowS(v1[0] + v2[0]),
                            narrowS(v1[1] + v2[1]),
                            narrowS(v1[2] + v2[2]),
                            narrowS(v1[3] + v2[3]),
                            narrowS(v1[4] + v2[4]),
                            narrowS(v1[5] + v2[5]),
                            narrowS(v1[6] + v2[6]),
                            narrowS(v1[7] + v2[7])
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_ADD_SAT_U(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var result =
                Value.i16ToVec(
                        new long[] {
                            addSatU(v1[0], v2[0]),
                            addSatU(v1[1], v2[1]),
                            addSatU(v1[2], v2[2]),
                            addSatU(v1[3], v2[3]),
                            addSatU(v1[4], v2[4]),
                            addSatU(v1[5], v2[5]),
                            addSatU(v1[6], v2[6]),
                            addSatU(v1[7], v2[7])
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void F32x4_EQ(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsFloats()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats()
                        .toArray();

        var result =
                Value.f32ToVec(
                        new long[] {
                            (v1[0] == v2[0]) ? 0xFFFFFFFFL : 0x0L,
                            (v1[1] == v2[1]) ? 0xFFFFFFFFL : 0x0L,
                            (v1[2] == v2[2]) ? 0xFFFFFFFFL : 0x0L,
                            (v1[3] == v2[3]) ? 0xFFFFFFFFL : 0x0L
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void UNARY(
            MStack stack, Function<LongVector, Vector> reinterpret, Function<Vector, Vector> fun) {
        var offset = stack.size() - 2;
        var v =
                reinterpret.apply(
                        LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset));

        var result = fun.apply(v).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void BINOP(
            MStack stack,
            Function<LongVector, Vector> reinterpret,
            BiFunction<Vector, Vector, Vector> fun) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                reinterpret.apply(
                        LongVector.fromArray(
                                LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0));
        var v2 =
                reinterpret.apply(
                        LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset));

        var result = fun.apply(v1, v2).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SUB(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v2.sub(v1).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void SH(MStack stack, BiFunction<LongVector, Long, LongVector> shl) {
        var s = stack.pop();
        var offset = stack.size() - 2;

        var result =
                shl.apply(LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset), s)
                        .toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void BOOL_OP(MStack stack, Function<LongVector, Boolean> condition) {
        var vHigh = stack.pop();
        var vLow = stack.pop();

        var result =
                condition.apply(
                        LongVector.fromArray(LongVector.SPECIES_128, new long[] {vLow, vHigh}, 0));

        if (result) {
            stack.push(BitOps.TRUE);
        } else {
            stack.push(BitOps.FALSE);
        }
    }

    private static void BITMASK(MStack stack, Function<LongVector, long[]> reduce) {
        var vHigh = stack.pop();
        var vLow = stack.pop();

        var vals =
                reduce.apply(
                        LongVector.fromArray(LongVector.SPECIES_128, new long[] {vLow, vHigh}, 0));

        var result = 0L;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] < 0) {
                result |= 1 << i;
            }
        }

        stack.push(result);
    }

    private static void V128_NOT(MStack stack) {
        var offset = stack.size() - 2;
        var not = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset).not();
        var res = not.toArray();

        System.arraycopy(res, 0, stack.array(), offset, 2);
    }

    private static void V128_BINOP(
            MStack stack, BiFunction<LongVector, LongVector, LongVector> binop) {
        var v1High = stack.pop();
        var v1Low = stack.pop();
        var offset = stack.size() - 2;
        var v1 = LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0);
        var v2 = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset);
        var res = binop.apply(v1, v2).toArray();

        System.arraycopy(res, 0, stack.array(), offset, 2);
    }

    private static void V128_ANY_TRUE(MStack stack) {
        var vHigh = stack.pop();
        var vLow = stack.pop();

        // TODO: check the spec!
        if (vLow != 0L || vHigh != 0L) {
            stack.push(BitOps.TRUE);
        } else {
            stack.push(BitOps.FALSE);
        }
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

    private static void I8x16(MStack stack, BiFunction<Byte, Byte, Long> op) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        long[] result = new long[16];
        for (int i = 0; i < 16; i++) {
            result[i] = op.apply(v1[i], v2[i]);
        }
        System.arraycopy(Value.i8ToVec(result), 0, stack.array(), offset, 2);
    }

    private static void I16x8(MStack stack, BiFunction<Short, Short, Long> op) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        long[] result = new long[8];
        for (int i = 0; i < 8; i++) {
            result[i] = op.apply(v1[i], v2[i]);
        }
        System.arraycopy(Value.i16ToVec(result), 0, stack.array(), offset, 2);
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

    private static void F32x4_CONVERT_I32x4_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_S(
                                                    (int) ((valHigh >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_S(
                                                    (int) ((valLow >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F64x2_PROMOTE_LOW_F32x4(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsFloats()
                        .toArray();

        stack.push(Value.floatToLong(v[0]));
        stack.push(Value.floatToLong(v[1]));
    }

    private static byte narrowS(short a) {
        if (a < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        } else if (a > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        } else {
            return (byte) a;
        }
    }

    private static short narrowS(int a) {
        if (a < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        } else if (a > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        } else {
            return (short) a;
        }
    }

    private static byte narrowU(short a) {
        if (a < 0) {
            return 0;
        } else if (a > 255) {
            return -1;
        } else {
            return (byte) a;
        }
    }

    private static short narrowU(int a) {
        if (a < 0) {
            return 0;
        } else if (a > 65535) {
            return -1;
        } else {
            return (short) a;
        }
    }

    private static void I8x16_NARROW_I16x8(MStack stack, Function<Short, Byte> narrow) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var result =
                Value.i8ToVec(
                        new long[] {
                            narrow.apply(v2[0]),
                            narrow.apply(v2[1]),
                            narrow.apply(v2[2]),
                            narrow.apply(v2[3]),
                            narrow.apply(v2[4]),
                            narrow.apply(v2[5]),
                            narrow.apply(v2[6]),
                            narrow.apply(v2[7]),
                            narrow.apply(v1[0]),
                            narrow.apply(v1[1]),
                            narrow.apply(v1[2]),
                            narrow.apply(v1[3]),
                            narrow.apply(v1[4]),
                            narrow.apply(v1[5]),
                            narrow.apply(v1[6]),
                            narrow.apply(v1[7]),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_NARROW_I32x4(MStack stack, Function<Integer, Short> narrow) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsInts()
                        .toArray();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsInts()
                        .toArray();

        var result =
                Value.i16ToVec(
                        new long[] {
                            narrow.apply(v2[0]),
                            narrow.apply(v2[1]),
                            narrow.apply(v2[2]),
                            narrow.apply(v2[3]),
                            narrow.apply(v1[0]),
                            narrow.apply(v1[1]),
                            narrow.apply(v1[2]),
                            narrow.apply(v1[3]),
                        });

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void F32x4_DEMOTE_LOW_F64x2_ZERO(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsDoubles()
                        .toArray();

        var vals =
                Value.f32ToVec(
                        new long[] {
                            Value.floatToLong((float) v[0]), Value.floatToLong((float) v[1]), 0, 0
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTMUL_LOW_I8x16_S(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var res = new long[8];
        for (int i = 0; i < 8; i++) {
            res[i] = v1[i] * v2[i];
        }
        var result = Value.i16ToVec(res);
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_EXTMUL_HIGH_I8x16_S(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var res = new long[8];
        for (int i = 0; i < 8; i++) {
            res[i] = v1[8 + i] * v2[8 + i];
        }
        var result = Value.i16ToVec(res);
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_EXTMUL_LOW_I8x16_U(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var res = new long[8];
        for (int i = 0; i < 8; i++) {
            res[i] = Byte.toUnsignedLong(v1[i]) * Byte.toUnsignedLong(v2[i]);
        }
        var result = Value.i16ToVec(res);
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_EXTMUL_HIGH_I8x16_U(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes()
                        .toArray();

        var res = new long[8];
        for (int i = 0; i < 8; i++) {
            res[i] = Byte.toUnsignedLong(v1[8 + i]) * Byte.toUnsignedLong(v2[8 + i]);
        }
        var result = Value.i16ToVec(res);
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static short roundQ15(int a) {
        if (a < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        } else if (a > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        } else {
            return (short) a;
        }
    }

    private static void I16x8_Q15MULR_SAT_S(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {val1Low, val1High}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsShorts()
                        .toArray();

        var res = new long[8];
        for (int i = 0; i < 8; i++) {
            // https://github.com/argon-lang/jawawasm/blob/0193bc0bbd1157d0de4d83c8139d317e638d44ed/engine/src/main/java/dev/argon/jawawasm/engine/StackFrame.java#L903-L910
            res[i] = roundQ15((v1[i] * v2[i] + (1 << 14)) >> 15);
        }
        var result = Value.i16ToVec(res);
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I16x8_EXTADD_PAIRWISE_I8x16_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals =
                Value.i16ToVec(
                        new long[] {
                            v[0] + v[1],
                            v[2] + v[3],
                            v[4] + v[5],
                            v[6] + v[7],
                            v[8] + v[9],
                            v[10] + v[11],
                            v[12] + v[13],
                            v[14] + v[15],
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTADD_PAIRWISE_I8x16_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals =
                Value.i16ToVec(
                        new long[] {
                            Byte.toUnsignedLong(v[0]) + Byte.toUnsignedLong(v[1]),
                            Byte.toUnsignedLong(v[2]) + Byte.toUnsignedLong(v[3]),
                            Byte.toUnsignedLong(v[4]) + Byte.toUnsignedLong(v[5]),
                            Byte.toUnsignedLong(v[6]) + Byte.toUnsignedLong(v[7]),
                            Byte.toUnsignedLong(v[8]) + Byte.toUnsignedLong(v[9]),
                            Byte.toUnsignedLong(v[10]) + Byte.toUnsignedLong(v[11]),
                            Byte.toUnsignedLong(v[12]) + Byte.toUnsignedLong(v[13]),
                            Byte.toUnsignedLong(v[14]) + Byte.toUnsignedLong(v[15]),
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTEND_LOW_I8x16_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals = Value.i16ToVec(new long[] {v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTEND_HIGH_I8x16_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals =
                Value.i16ToVec(new long[] {v[8], v[9], v[10], v[11], v[12], v[13], v[14], v[15]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I32x4_EXTEND_LOW_I16x8_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var vals = Value.i32ToVec(new long[] {v[0], v[1], v[2], v[3]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I32x4_EXTEND_HIGH_I16x8_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var vals = Value.i32ToVec(new long[] {v[4], v[5], v[6], v[7]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I32x4_EXTEND_LOW_I16x8_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var vals =
                Value.i32ToVec(
                        new long[] {
                            Short.toUnsignedLong(v[0]),
                            Short.toUnsignedLong(v[1]),
                            Short.toUnsignedLong(v[2]),
                            Short.toUnsignedLong(v[3])
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I32x4_EXTEND_HIGH_I16x8_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsShorts()
                        .toArray();

        var vals =
                Value.i32ToVec(
                        new long[] {
                            Short.toUnsignedLong(v[4]),
                            Short.toUnsignedLong(v[5]),
                            Short.toUnsignedLong(v[6]),
                            Short.toUnsignedLong(v[7])
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I64x2_EXTEND_HIGH_I32x4_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsInts()
                        .toArray();

        var vals = Value.i64ToVec(new long[] {v[2], v[3]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I64x2_EXTEND_LOW_I32x4_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsInts()
                        .toArray();

        var vals = Value.i64ToVec(new long[] {v[0], v[1]});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I64x2_EXTEND_HIGH_I32x4_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsInts()
                        .toArray();

        var vals =
                Value.i64ToVec(
                        new long[] {Integer.toUnsignedLong(v[2]), Integer.toUnsignedLong(v[3])});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I64x2_EXTEND_LOW_I32x4_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsInts()
                        .toArray();

        var vals =
                Value.i64ToVec(
                        new long[] {Integer.toUnsignedLong(v[0]), Integer.toUnsignedLong(v[1])});
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTEND_LOW_I8x16_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals =
                Value.i16ToVec(
                        new long[] {
                            Byte.toUnsignedLong(v[0]),
                            Byte.toUnsignedLong(v[1]),
                            Byte.toUnsignedLong(v[2]),
                            Byte.toUnsignedLong(v[3]),
                            Byte.toUnsignedLong(v[4]),
                            Byte.toUnsignedLong(v[5]),
                            Byte.toUnsignedLong(v[6]),
                            Byte.toUnsignedLong(v[7])
                        });
        for (var val : vals) {
            stack.push(val);
        }
    }

    private static void I16x8_EXTEND_HIGH_I8x16_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {valLow, valHigh}, 0)
                        .reinterpretAsBytes()
                        .toArray();

        var vals =
                Value.i16ToVec(
                        new long[] {
                            Byte.toUnsignedLong(v[8]),
                            Byte.toUnsignedLong(v[9]),
                            Byte.toUnsignedLong(v[10]),
                            Byte.toUnsignedLong(v[11]),
                            Byte.toUnsignedLong(v[12]),
                            Byte.toUnsignedLong(v[13]),
                            Byte.toUnsignedLong(v[14]),
                            Byte.toUnsignedLong(v[15])
                        });
        for (var val : vals) {
            stack.push(val);
        }
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
