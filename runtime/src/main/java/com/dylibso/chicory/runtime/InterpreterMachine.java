package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.List;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
class InterpreterMachine implements Machine {

    private final MStack stack;

    private final ArrayDeque<StackFrame> callStack;

    private final Instance instance;

    public InterpreterMachine(Instance instance) {
        this.instance = instance;
        stack = new MStack();
        this.callStack = new ArrayDeque<>();
    }

    @Override
    public Value[] call(int funcId, Value[] args) throws ChicoryException {
        return call(stack, instance, callStack, funcId, args, null, true);
    }

    public static Value[] call(
            MStack stack,
            Instance instance,
            ArrayDeque<StackFrame> callStack,
            int funcId,
            Value[] args,
            FunctionType callType,
            boolean popResults)
            throws ChicoryException {

        checkInterruption();
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);

        if (callType != null) {
            verifyIndirectCall(type, callType);
        }

        var func = instance.function(funcId);
        if (func != null) {
            var stackFrame =
                    new StackFrame(func.instructions(), instance, funcId, args, func.localTypes());
            stackFrame.pushCtrl(OpCode.CALL, 0, type.returns().size(), stack.size());
            callStack.push(stackFrame);

            eval(stack, instance, callStack);
        } else {
            var stackFrame = new StackFrame(instance, funcId, args, List.of());
            stackFrame.pushCtrl(OpCode.CALL, 0, type.returns().size(), stack.size());
            callStack.push(stackFrame);

            var results = instance.callHostFunction(funcId, args);
            // a host function can return null or an array of ints
            // which we will push onto the stack
            if (results != null) {
                for (var result : results) {
                    stack.push(result);
                }
            }
        }

        if (!callStack.isEmpty()) {
            callStack.pop();
        }

        if (!popResults) {
            return null;
        }

        if (type.returns().isEmpty()) return null;
        if (stack.size() == 0) return null;

        var totalResults = type.returns().size();
        var results = new Value[totalResults];
        for (var i = totalResults - 1; i >= 0; i--) {
            results[i] = stack.pop();
        }
        return results;
    }

    interface ExecuteInstruction {
        void execute(
                StackFrame frame,
                MStack stack,
                Instance instance,
                ArrayDeque<StackFrame> callStack,
                Instruction instruction,
                long[] operands)
                throws ChicoryException;
    }

    static void eval(MStack stack, Instance instance, ArrayDeque<StackFrame> callStack)
            throws ChicoryException {

        try {
            var frame = callStack.peek();

            while (!frame.terminated()) {
                if (frame.shouldReturn()) return;
                var instruction = frame.loadCurrentInstruction();
                //                LOGGER.log(
                //                        System.Logger.Level.DEBUG,
                //                        "func="
                //                                + frame.funcId
                //                                + "@"
                //                                + frame.pc
                //                                + ": "
                //                                + instruction
                //                                + " stack="
                //                                + stack);
                var opcode = instruction.opcode();
                var operands = instruction.operands();
                instance.onExecution(instruction, operands, stack);

                var exec = instructions[opcode.ordinal()];
                if (exec == null) {
                    throw new RuntimeException(
                            "Machine doesn't recognize Instruction " + instruction);
                }
                exec.execute(frame, stack, instance, callStack, instruction, operands);
            }
        } catch (ChicoryException e) {
            // propagate ChicoryExceptions
            throw e;
        } catch (StackOverflowError e) {
            throw new ChicoryException("call stack exhausted", e);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("undefined element " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        }
    }

    static ExecuteInstruction[] instructions = new ExecuteInstruction[OpCode.values().length];

    static {
        instructions[OpCode.UNREACHABLE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    throw new TrapException("Trapped on unreachable instruction", callStack);
                };
        instructions[OpCode.NOP.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    // do nothing
                };
        instructions[OpCode.I32_GE_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_GE_U(a, b)));
                };
        instructions[OpCode.I64_GT_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_GT_U(a, b)));
                };
        instructions[OpCode.I32_GE_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_GE_S(a, b)));
                };
        instructions[OpCode.I64_GE_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_GE_U(a, b)));
                };
        instructions[OpCode.I64_GE_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_GE_S(a, b)));
                };
        instructions[OpCode.I32_LE_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_LE_S(a, b)));
                };
        instructions[OpCode.I32_LE_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_LE_U(a, b)));
                };
        instructions[OpCode.I64_LE_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_LE_S(a, b)));
                };
        instructions[OpCode.I64_LE_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_LE_U(a, b)));
                };
        instructions[OpCode.F32_EQ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_EQ(a, b)));
                };
        instructions[OpCode.F64_EQ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_EQ(a, b)));
                };
        instructions[OpCode.I32_CLZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_CLZ(tos)));
                };
        instructions[OpCode.I32_CTZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_CTZ(tos)));
                };
        instructions[OpCode.I32_POPCNT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_POPCNT(tos)));
                };
        instructions[OpCode.I32_ADD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(a + b));
                };
        instructions[OpCode.I64_ADD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(a + b));
                };
        instructions[OpCode.I32_SUB.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(b - a));
                };
        instructions[OpCode.I64_SUB.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(b - a));
                };
        instructions[OpCode.I32_MUL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(a * b));
                };
        instructions[OpCode.I64_MUL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(a * b));
                };
        instructions[OpCode.I32_DIV_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_DIV_S(a, b)));
                };
        instructions[OpCode.I32_DIV_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_DIV_U(a, b)));
                };
        instructions[OpCode.I64_EXTEND_8_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_EXTEND_8_S(tos)));
                };
        instructions[OpCode.I64_EXTEND_16_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_EXTEND_16_S(tos)));
                };
        instructions[OpCode.I64_EXTEND_32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_EXTEND_32_S(tos)));
                };
        instructions[OpCode.F64_CONVERT_I64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_CONVERT_I64_U(tos)));
                };
        instructions[OpCode.F64_CONVERT_I32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_CONVERT_I32_U(tos)));
                };
        instructions[OpCode.F64_CONVERT_I32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_CONVERT_I32_S(tos)));
                };
        instructions[OpCode.I32_EXTEND_8_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_EXTEND_8_S(tos)));
                };
        instructions[OpCode.F64_NEAREST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_NEAREST(val)));
                };
        instructions[OpCode.F32_NEAREST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_NEAREST(val)));
                };
        instructions[OpCode.F64_TRUNC.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_TRUNC(val)));
                };
        instructions[OpCode.F64_CEIL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_CEIL(val)));
                };
        instructions[OpCode.F32_CEIL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_CEIL(val)));
                };
        instructions[OpCode.F64_FLOOR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_FLOOR(val)));
                };
        instructions[OpCode.F32_FLOOR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_FLOOR(val)));
                };
        instructions[OpCode.F64_SQRT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_SQRT(val)));
                };
        instructions[OpCode.F32_SQRT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_SQRT(val)));
                };
        instructions[OpCode.F64_MAX.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_MAX(a, b)));
                };
        instructions[OpCode.F32_MAX.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_MAX(a, b)));
                };
        instructions[OpCode.F64_MIN.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_MIN(a, b)));
                };
        instructions[OpCode.F32_MIN.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_MIN(a, b)));
                };
        instructions[OpCode.F64_DIV.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(b / a));
                };
        instructions[OpCode.F32_DIV.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(b / a));
                };
        instructions[OpCode.F64_MUL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(b * a));
                };
        instructions[OpCode.F32_MUL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(b * a));
                };
        instructions[OpCode.F64_SUB.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(b - a));
                };
        instructions[OpCode.F32_SUB.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(b - a));
                };
        instructions[OpCode.F64_ADD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asDouble();
                    var b = stack.pop().asDouble();
                    stack.push(Value.fromDouble(a + b));
                };
        instructions[OpCode.F32_ADD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asFloat();
                    var b = stack.pop().asFloat();
                    stack.push(Value.fromFloat(a + b));
                };
        instructions[OpCode.I32_ROTR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asInt();
                    var v = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_ROTR(v, c)));
                };
        instructions[OpCode.I32_ROTL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asInt();
                    var v = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_ROTL(v, c)));
                };
        instructions[OpCode.I32_SHR_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asInt();
                    var v = stack.pop().asInt();
                    stack.push(Value.i32(v >>> c));
                };
        instructions[OpCode.I32_SHR_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asInt();
                    var v = stack.pop().asInt();
                    stack.push(Value.i32(v >> c));
                };
        instructions[OpCode.I32_SHL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asInt();
                    var v = stack.pop().asInt();
                    stack.push(Value.i32(v << c));
                };
        instructions[OpCode.I32_XOR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(a ^ b));
                };
        instructions[OpCode.I32_OR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(a | b));
                };
        instructions[OpCode.I32_AND.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(a & b));
                };
        instructions[OpCode.I64_POPCNT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_POPCNT(tos)));
                };
        instructions[OpCode.I64_CTZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop();
                    stack.push(Value.i64(OpcodeImpl.I64_CTZ(tos.asLong())));
                };
        instructions[OpCode.I64_CLZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop();
                    stack.push(Value.i64(OpcodeImpl.I64_CLZ(tos.asLong())));
                };
        instructions[OpCode.I64_ROTR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asLong();
                    var v = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_ROTR(v, c)));
                };
        instructions[OpCode.I64_ROTL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asLong();
                    var v = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_ROTL(v, c)));
                };
        instructions[OpCode.I64_REM_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_REM_U(a, b)));
                };
        instructions[OpCode.I64_REM_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_REM_S(a, b)));
                };
        instructions[OpCode.I64_SHR_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asLong();
                    var v = stack.pop().asLong();
                    stack.push(Value.i64(v >>> c));
                };
        instructions[OpCode.I64_SHR_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asLong();
                    var v = stack.pop().asLong();
                    stack.push(Value.i64(v >> c));
                };
        instructions[OpCode.I64_SHL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var c = stack.pop().asLong();
                    var v = stack.pop().asLong();
                    stack.push(Value.i64(v << c));
                };
        instructions[OpCode.I64_XOR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(a ^ b));
                };
        instructions[OpCode.I64_OR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(a | b));
                };
        instructions[OpCode.I64_AND.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i64(a & b));
                };
        instructions[OpCode.I32_REM_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_REM_U(a, b)));
                };
        instructions[OpCode.I32_REM_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_REM_S(a, b)));
                };
        instructions[OpCode.I64_DIV_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_DIV_U(a, b)));
                };
        instructions[OpCode.I64_DIV_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i64(OpcodeImpl.I64_DIV_S(a, b)));
                };
        instructions[OpCode.I64_GT_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_GT_S(a, b)));
                };
        instructions[OpCode.I32_GT_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_GT_U(a, b)));
                };
        instructions[OpCode.I32_GT_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_GT_S(a, b)));
                };
        instructions[OpCode.I64_LT_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_LT_U(a, b)));
                };
        instructions[OpCode.I64_LT_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asLong();
                    var a = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_LT_S(a, b)));
                };
        instructions[OpCode.I32_LT_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_LT_U(a, b)));
                };
        instructions[OpCode.I32_LT_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asInt();
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_LT_S(a, b)));
                };
        instructions[OpCode.I64_EQZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_EQZ(a)));
                };
        instructions[OpCode.I32_EQZ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_EQZ(a)));
                };
        instructions[OpCode.I64_NE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_NE(a, b)));
                };
        instructions[OpCode.I32_NE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_NE(a, b)));
                };
        instructions[OpCode.I64_EQ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asLong();
                    var b = stack.pop().asLong();
                    stack.push(Value.i32(OpcodeImpl.I64_EQ(a, b)));
                };
        instructions[OpCode.I32_EQ.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var a = stack.pop().asInt();
                    var b = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_EQ(a, b)));
                };
        instructions[OpCode.MEMORY_SIZE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var sz = instance.memory().pages();
                    stack.push(Value.i32(sz));
                };
        instructions[OpCode.I64_STORE32.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asLong();
                    var ptr = (int) (operands[1] + stack.pop().asInt());
                    instance.memory().writeI32(ptr, (int) value);
                };
        instructions[OpCode.I32_STORE8.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asByte();
                    var ptr = (int) (operands[1] + stack.pop().asInt());
                    instance.memory().writeByte(ptr, value);
                };
        instructions[OpCode.I64_STORE8.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asByte();
                    var ptr = (int) (operands[1] + stack.pop().asInt());
                    instance.memory().writeByte(ptr, value);
                };
        instructions[OpCode.F64_PROMOTE_F32.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop();
                    stack.push(Value.fromDouble(tos.asFloat()));
                };
        instructions[OpCode.F64_REINTERPRET_I64.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    long tos = stack.pop().asLong();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_REINTERPRET_I64(tos)));
                };
        instructions[OpCode.I32_WRAP_I64.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop();
                    stack.push(Value.i32(tos.asInt()));
                };
        instructions[OpCode.I64_EXTEND_I32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop();
                    stack.push(Value.i64(tos.asInt()));
                };
        instructions[OpCode.I64_EXTEND_I32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    int tos = stack.pop().asInt();
                    stack.push(Value.i64(OpcodeImpl.I64_EXTEND_I32_U(tos)));
                };
        instructions[OpCode.I32_REINTERPRET_F32.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    float tos = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.I32_REINTERPRET_F32(tos)));
                };
        instructions[OpCode.I64_REINTERPRET_F64.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    double tos = stack.pop().asDouble();
                    stack.push(Value.i64(OpcodeImpl.I64_REINTERPRET_F64(tos)));
                };
        instructions[OpCode.F32_REINTERPRET_I32.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    int tos = stack.pop().asInt();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_REINTERPRET_I32(tos)));
                };
        instructions[OpCode.F32_DEMOTE_F64.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();

                    stack.push(Value.fromFloat((float) val));
                };
        instructions[OpCode.F32_CONVERT_I32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_CONVERT_I32_S(tos)));
                };
        instructions[OpCode.I32_EXTEND_16_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.i32(OpcodeImpl.I32_EXTEND_16_S(tos)));
                };
        instructions[OpCode.I64_TRUNC_F64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    double tos = stack.pop().asDouble();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_F64_S(tos)));
                };
        instructions[OpCode.F32_COPYSIGN.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_COPYSIGN(a, b)));
                };
        instructions[OpCode.F32_ABS.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_ABS(val)));
                };
        instructions[OpCode.F64_ABS.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_ABS(val)));
                };
        instructions[OpCode.F32_NE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_NE(a, b)));
                };
        instructions[OpCode.F64_NE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_NE(a, b)));
                };
        instructions[OpCode.F32_LT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_LT(a, b)));
                };
        instructions[OpCode.F64_LT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_LT(a, b)));
                };
        instructions[OpCode.F32_LE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_LE(a, b)));
                };
        instructions[OpCode.F64_LE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_LE(a, b)));
                };
        instructions[OpCode.F32_GE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_GE(a, b)));
                };
        instructions[OpCode.F64_GE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_GE(a, b)));
                };
        instructions[OpCode.F32_GT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asFloat();
                    var a = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.F32_GT(a, b)));
                };
        instructions[OpCode.F64_GT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.F64_GT(a, b)));
                };
        instructions[OpCode.F32_CONVERT_I32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asInt();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_CONVERT_I32_U(tos)));
                };
        instructions[OpCode.F32_CONVERT_I64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_CONVERT_I64_S(tos)));
                };
        instructions[OpCode.REF_NULL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var type = ValueType.forId((int) operands[0]);
                    stack.push(new Value(type, (long) REF_NULL_VALUE));
                };
        instructions[OpCode.ELEM_DROP.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var x = (int) operands[0];
                    instance.setElement(x, null);
                };
        instructions[OpCode.REF_IS_NULL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop();
                    stack.push(
                            val.equals(Value.EXTREF_NULL) || val.equals(Value.FUNCREF_NULL)
                                    ? Value.TRUE
                                    : Value.FALSE);
                };
        instructions[OpCode.DATA_DROP.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var segment = (int) operands[0];
                    instance.memory().drop(segment);
                };
        instructions[OpCode.F64_CONVERT_I64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_CONVERT_I64_S(tos)));
                };
        instructions[OpCode.TABLE_GROW.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableidx = (int) operands[0];
                    var table = instance.table(tableidx);

                    var size = stack.pop().asInt();
                    var valValue = stack.pop();
                    var val = valValue.asExtRef();

                    var res = table.grow(size, val, instance);
                    stack.push(Value.i32(res));
                };
        instructions[OpCode.TABLE_SIZE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableidx = (int) operands[0];
                    var table = instance.table(tableidx);

                    stack.push(Value.i32(table.size()));
                };
        instructions[OpCode.TABLE_FILL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableidx = (int) operands[0];

                    var size = stack.pop().asInt();
                    var val = stack.pop().asExtRef();
                    var offset = stack.pop().asInt();

                    OpcodeImpl.TABLE_FILL(instance, tableidx, size, val, offset);
                };
        instructions[OpCode.TABLE_COPY.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableidxSrc = (int) operands[1];
                    var tableidxDst = (int) operands[0];

                    var size = stack.pop().asInt();
                    var s = stack.pop().asInt();
                    var d = stack.pop().asInt();

                    OpcodeImpl.TABLE_COPY(instance, tableidxSrc, tableidxDst, size, s, d);
                };
        instructions[OpCode.MEMORY_COPY.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var memidxSrc = (int) operands[0];
                    var memidxDst = (int) operands[1];
                    if (memidxDst != 0 && memidxSrc != 0)
                        throw new WASMRuntimeException(
                                "We don't support non zero index for memory: "
                                        + memidxSrc
                                        + " "
                                        + memidxDst);
                    var size = stack.pop().asInt();
                    var offset = stack.pop().asInt();
                    var destination = stack.pop().asInt();
                    instance.memory().copy(destination, offset, size);
                };
        instructions[OpCode.TABLE_INIT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableidx = (int) operands[1];
                    var elementidx = (int) operands[0];

                    var size = stack.pop().asInt();
                    var elemidx = stack.pop().asInt();
                    var offset = stack.pop().asInt();

                    OpcodeImpl.TABLE_INIT(instance, tableidx, elementidx, size, elemidx, offset);
                };
        instructions[OpCode.MEMORY_INIT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var segmentId = (int) operands[0];
                    var memidx = (int) operands[1];
                    if (memidx != 0)
                        throw new WASMRuntimeException(
                                "We don't support non zero index for memory: " + memidx);
                    var size = stack.pop().asInt();
                    var offset = stack.pop().asInt();
                    var destination = stack.pop().asInt();
                    instance.memory().initPassiveSegment(segmentId, destination, offset, size);
                };
        instructions[OpCode.I64_TRUNC_F32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_F32_S(tos)));
                };
        instructions[OpCode.I32_TRUNC_F64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    double tos = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_F64_U(tos)));
                };
        instructions[OpCode.I32_TRUNC_F64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_F64_S(tos)));
                };
        instructions[OpCode.I64_TRUNC_SAT_F64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    double tos = stack.pop().asDouble();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_SAT_F64_U(tos)));
                };
        instructions[OpCode.I64_TRUNC_SAT_F64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asDouble();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_SAT_F64_S(tos)));
                };
        instructions[OpCode.I64_TRUNC_SAT_F32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_SAT_F32_U(tos)));
                };
        instructions[OpCode.I64_TRUNC_SAT_F32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_SAT_F32_S(tos)));
                };
        instructions[OpCode.I64_TRUNC_F64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asDouble();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_F64_U(tos)));
                };
        instructions[OpCode.I64_TRUNC_F32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i64(OpcodeImpl.I64_TRUNC_F32_U(tos)));
                };
        instructions[OpCode.F32_CONVERT_I64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asLong();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_CONVERT_I64_U(tos)));
                };
        instructions[OpCode.I32_TRUNC_F32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_F32_U(tos)));
                };
        instructions[OpCode.I32_TRUNC_SAT_F64_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    double tos = Double.longBitsToDouble(stack.pop().asLong());
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_SAT_F64_U(tos)));
                };
        instructions[OpCode.I32_TRUNC_SAT_F64_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asDouble();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_SAT_F64_S(tos)));
                };
        instructions[OpCode.I32_TRUNC_SAT_F32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_SAT_F32_U(tos)));
                };
        instructions[OpCode.I32_TRUNC_SAT_F32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_SAT_F32_S(tos)));
                };
        instructions[OpCode.I32_TRUNC_F32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    float tos = stack.pop().asFloat();
                    stack.push(Value.i32(OpcodeImpl.I32_TRUNC_F32_S(tos)));
                };
        instructions[OpCode.F64_COPYSIGN.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var b = stack.pop().asDouble();
                    var a = stack.pop().asDouble();
                    stack.push(Value.fromDouble(OpcodeImpl.F64_COPYSIGN(a, b)));
                };
        instructions[OpCode.F32_TRUNC.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var val = stack.pop().asFloat();
                    stack.push(Value.fromFloat(OpcodeImpl.F32_TRUNC(val)));
                };
        instructions[OpCode.CALL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var funcId = (int) operands[0];
                    var typeId = instance.functionType(funcId);
                    var type = instance.type(typeId);
                    // given a list of param types, let's pop those params off the stack
                    // and pass as args to the function call
                    var args = extractArgsForParams(stack, type.params());
                    call(stack, instance, callStack, funcId, args, type, false);
                };
        instructions[OpCode.F64_NEG.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asDouble();
                    stack.push(Value.fromDouble(-tos));
                };
        instructions[OpCode.F32_NEG.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tos = stack.pop().asFloat();
                    stack.push(Value.fromFloat(-tos));
                };
        instructions[OpCode.MEMORY_FILL.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var memidx = (int) operands[0];
                    if (memidx != 0) {
                        throw new WASMRuntimeException(
                                "We don't support multiple memories just yet");
                    }
                    var size = stack.pop().asInt();
                    var val = stack.pop().asByte();
                    var offset = stack.pop().asInt();
                    var end = (size + offset);
                    instance.memory().fill(val, offset, end);
                };
        instructions[OpCode.MEMORY_GROW.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var size = stack.pop().asInt();
                    var nPages = instance.memory().grow(size);
                    stack.push(Value.i32(nPages));
                };

        instructions[OpCode.F64_STORE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asDouble();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeF64(ptr, value);
                };
        instructions[OpCode.F32_STORE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asFloat();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeF32(ptr, value);
                };
        instructions[OpCode.I64_STORE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asLong();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeLong(ptr, value);
                };
        instructions[OpCode.I32_STORE16.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asShort();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeShort(ptr, value);
                };
        instructions[OpCode.I64_STORE16.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asShort();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeShort(ptr, value);
                };
        instructions[OpCode.I32_STORE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var value = stack.pop().asInt();
                    var ptr = readMemPtr(stack, operands);
                    instance.memory().writeI32(ptr, value);
                };
        instructions[OpCode.I64_LOAD32_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readU32(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I64_LOAD32_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI32(ptr);
                    // TODO this is a bit hacky
                    stack.push(Value.i64(val.asInt()));
                };
        instructions[OpCode.I64_LOAD16_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readU16(ptr);
                    // TODO this is a bit hacky
                    stack.push(Value.i64(val.asInt()));
                };
        instructions[OpCode.I32_LOAD16_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readU16(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I64_LOAD16_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI16(ptr);
                    // TODO this is a bit hacky
                    stack.push(Value.i64(val.asInt()));
                };
        instructions[OpCode.I32_LOAD16_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI16(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I64_LOAD8_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readU8(ptr);
                    // TODO a bit hacky
                    stack.push(Value.i64(val.asInt()));
                };
        instructions[OpCode.I32_LOAD8_U.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readU8(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I64_LOAD8_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI8(ptr);
                    // TODO a bit hacky
                    stack.push(Value.i64(val.asInt()));
                };
        instructions[OpCode.I32_LOAD8_S.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI8(ptr);
                    stack.push(val);
                };
        instructions[OpCode.F64_LOAD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readF64(ptr);
                    stack.push(val);
                };
        instructions[OpCode.F32_LOAD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readF32(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I64_LOAD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI64(ptr);
                    stack.push(val);
                };
        instructions[OpCode.I32_LOAD.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ptr = readMemPtr(stack, operands);
                    var val = instance.memory().readI32(ptr);
                    stack.push(val);
                };
        instructions[OpCode.TABLE_SET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var idx = (int) operands[0];
                    var table = instance.table(idx);

                    var value = stack.pop().asExtRef();
                    var i = stack.pop().asInt();
                    table.setRef(i, value, instance);
                };
        instructions[OpCode.TABLE_GET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var idx = (int) operands[0];
                    var i = stack.pop().asInt();
                    stack.push(OpcodeImpl.TABLE_GET(instance, idx, i));
                };
        instructions[OpCode.GLOBAL_SET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var id = (int) operands[0];
                    var val = stack.pop();
                    instance.writeGlobal(id, val);
                };
        instructions[OpCode.GLOBAL_GET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    int idx = (int) operands[0];
                    var val = instance.readGlobal(idx);

                    stack.push(val);
                };
        instructions[OpCode.SELECT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var pred = stack.pop().asInt();
                    var b = stack.pop();
                    var a = stack.pop();
                    if (pred == 0) {
                        stack.push(b);
                    } else {
                        stack.push(a);
                    }
                };
        instructions[OpCode.SELECT_T.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var pred = stack.pop().asInt();
                    var b = stack.pop();
                    var a = stack.pop();
                    if (pred == 0) {
                        stack.push(b);
                    } else {
                        stack.push(a);
                    }
                };
        instructions[OpCode.CALL_INDIRECT.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var tableIdx = (int) operands[1];
                    var table = instance.table(tableIdx);

                    var typeId = (int) operands[0];
                    int funcTableIdx = stack.pop().asInt();
                    int funcId = table.ref(funcTableIdx).asFuncRef();
                    var tableInstance = table.instance(funcTableIdx);
                    if (tableInstance != null) {
                        instance = tableInstance;
                    }
                    if (funcId == REF_NULL_VALUE) {
                        throw new ChicoryException("uninitialized element " + funcTableIdx);
                    }
                    var type = instance.type(typeId);

                    // given a list of param types, let's pop those params off the stack
                    // and pass as args to the function call
                    var args = extractArgsForParams(stack, type.params());
                    call(stack, instance, callStack, funcId, args, type, false);
                };
        instructions[OpCode.BLOCK.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var paramsSize = numberOfParams(instance, instruction);
                    var returnsSize = numberOfValuesToReturn(instance, instruction);
                    frame.pushCtrl(
                            instruction.opcode(),
                            paramsSize,
                            returnsSize,
                            stack.size() - paramsSize);
                };
        instructions[OpCode.LOOP.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var paramsSize = numberOfParams(instance, instruction);
                    var returnsSize = numberOfValuesToReturn(instance, instruction);
                    frame.pushCtrl(
                            instruction.opcode(),
                            paramsSize,
                            returnsSize,
                            stack.size() - paramsSize);
                };
        instructions[OpCode.IF.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var predValue = stack.pop();
                    var paramsSize = numberOfParams(instance, instruction);
                    var returnsSize = numberOfValuesToReturn(instance, instruction);
                    frame.pushCtrl(
                            instruction.opcode(),
                            paramsSize,
                            returnsSize,
                            stack.size() - paramsSize);

                    frame.jumpTo(
                            predValue.asInt() == 0
                                    ? instruction.labelFalse()
                                    : instruction.labelTrue());
                };
        instructions[OpCode.BR.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    checkInterruption();
                    ctrlJump(frame, stack, (int) instruction.operands()[0]);
                    frame.jumpTo(instruction.labelTrue());
                };
        instructions[OpCode.BR_TABLE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var predValue = stack.pop();
                    var pred = predValue.asInt();

                    var defaultIdx = instruction.operands().length - 1;
                    if (pred < 0 || pred >= defaultIdx) {
                        // choose default
                        ctrlJump(frame, stack, (int) instruction.operands()[defaultIdx]);
                        frame.jumpTo(instruction.labelTable()[defaultIdx]);
                    } else {
                        ctrlJump(frame, stack, (int) instruction.operands()[pred]);
                        frame.jumpTo(instruction.labelTable()[pred]);
                    }
                };
        instructions[OpCode.BR_IF.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var predValue = stack.pop();
                    var pred = predValue.asInt();

                    if (pred == 0) {
                        frame.jumpTo(instruction.labelFalse());
                    } else {
                        ctrlJump(frame, stack, (int) instruction.operands()[0]);
                        frame.jumpTo(instruction.labelTrue());
                    }
                };
        instructions[OpCode.END.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    var ctrlFrame = frame.popCtrl();
                    StackFrame.doControlTransfer(ctrlFrame, stack);

                    // if this is the last end, then we're done with
                    // the function
                    if (frame.isLastBlock()) {
                        frame.shouldReturn(true);
                    }
                };
        instructions[OpCode.RETURN.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    // RETURN doesn't pass through the END
                    var ctrlFrame = frame.popCtrlTillCall();
                    StackFrame.doControlTransfer(ctrlFrame, stack);

                    frame.shouldReturn(true);
                };
        instructions[OpCode.ELSE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    frame.jumpTo(instruction.labelTrue());
                };
        instructions[OpCode.I32_CONST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(Value.i32(operands[0]));
                };
        instructions[OpCode.I64_CONST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(Value.i64(operands[0]));
                };
        instructions[OpCode.F32_CONST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(Value.f32(operands[0]));
                };
        instructions[OpCode.F64_CONST.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(Value.f64(operands[0]));
                };
        instructions[OpCode.REF_FUNC.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(Value.funcRef((int) operands[0]));
                };
        instructions[OpCode.LOCAL_GET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.push(frame.local((int) operands[0]));
                };
        instructions[OpCode.LOCAL_SET.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    frame.setLocal((int) operands[0], stack.pop());
                };
        instructions[OpCode.LOCAL_TEE.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    // here we peek instead of pop, leaving it on the stack
                    frame.setLocal((int) operands[0], stack.peek());
                };
        instructions[OpCode.DROP.ordinal()] =
                (frame, stack, instance, callStack, instruction, operands) -> {
                    stack.pop();
                };
    }

    private static int numberOfParams(Instance instance, Instruction scope) {
        var typeId = (int) scope.operands()[0];
        if (typeId == 0x40) { // epsilon
            return 0;
        }
        if (ValueType.isValid(typeId)) {
            return 0;
        }
        return instance.type(typeId).params().size();
    }

    private static int numberOfValuesToReturn(Instance instance, Instruction scope) {
        if (scope.opcode() == OpCode.END) {
            return 0;
        }
        var typeId = (int) scope.operands()[0];
        if (typeId == 0x40) { // epsilon
            return 0;
        }
        if (ValueType.isValid(typeId)) {
            return 1;
        }
        return instance.type(typeId).returns().size();
    }

    private static int readMemPtr(MStack stack, long[] operands) {
        int offset = stack.pop().asInt();
        if (operands[1] < 0 || operands[1] >= Integer.MAX_VALUE || offset < 0) {
            throw new WASMRuntimeException("out of bounds memory access");
        }
        return (int) (operands[1] + offset);
    }

    private static void ctrlJump(StackFrame frame, MStack stack, int n) {
        var ctrlFrame = frame.popCtrl(n);
        frame.pushCtrl(ctrlFrame);
        // a LOOP jumps back to the first instruction without passing through an END
        if (ctrlFrame.opCode == OpCode.LOOP) {
            StackFrame.doControlTransfer(ctrlFrame, stack);
        }
    }

    @Override
    public List<StackFrame> getStackTrace() {
        return List.copyOf(callStack);
    }

    static Value[] extractArgsForParams(MStack stack, List<ValueType> params) {
        if (params == null) {
            return Value.EMPTY_VALUES;
        }
        var args = new Value[params.size()];
        for (var i = params.size(); i > 0; i--) {
            var p = stack.pop();
            var t = params.get(i - 1);
            if (p.type() != t) {
                // Similar to what is happening in WaZero
                // https://github.com/tetratelabs/wazero/blob/36676928d22ab92c34299eb0dca7608c92c94b22/internal/wasm/gofunc.go#L109
                switch (t) {
                    case I32:
                    case I64:
                    case F32:
                    case F64:
                        p = new Value(t, p.asLong());
                        break;
                    default:
                        throw new RuntimeException("Type error when extracting args. Found: " + t);
                }
            }
            args[i - 1] = p;
        }
        return args;
    }

    protected static void verifyIndirectCall(FunctionType actual, FunctionType expected)
            throws ChicoryException {
        if (!actual.typesMatch(expected)) {
            throw new ChicoryException("indirect call type mismatch");
        }
    }

    /**
     * Terminate WASM execution if requested.
     * This is called at the start of each call and at any potentially backwards branches.
     * Forward branches and other non-branch instructions are not checked, as the
     * execution will run until it eventually reaches a termination point.
     */
    private static void checkInterruption() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ChicoryException("Thread interrupted");
        }
    }
}
