package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.PassiveElement;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
class Machine {

    private final MStack stack;

    private final ArrayDeque<StackFrame> callStack;

    private final Instance instance;

    public Machine(Instance instance) {
        this.instance = instance;
        stack = new MStack();
        this.callStack = new ArrayDeque<>();
    }

    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        return call(stack, instance, callStack, funcId, args, null, popResults);
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

        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);

        if (callType != null) {
            verifyIndirectCall(type, callType);
        }

        var func = instance.function(funcId);
        if (func != null) {
            callStack.push(
                    new StackFrame(func.instructions(), instance, funcId, args, func.localTypes()));
            eval(stack, instance, callStack);
        } else {
            callStack.push(new StackFrame(instance, funcId, args, List.of()));
            var imprt = instance.imports().function(funcId);
            if (imprt == null) {
                throw new ChicoryException("Missing host import, number: " + funcId);
            }
            var hostFunc = imprt.handle();
            var results = hostFunc.apply(instance, args);
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

    static void eval(MStack stack, Instance instance, ArrayDeque<StackFrame> callStack)
            throws ChicoryException {

        try {
            var frame = callStack.peek();
            boolean shouldReturn = false;

            loop:
            while (!frame.terminated()) {
                if (shouldReturn) return;
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
                switch (opcode) {
                    case UNREACHABLE:
                        throw new TrapException("Trapped on unreachable instruction", callStack);
                    case NOP:
                        break;
                    case LOOP:
                    case BLOCK:
                        BLOCK(frame, stack);
                        break;
                    case IF:
                        IF(frame, stack, instruction);
                        break;
                    case ELSE:
                    case BR:
                        prepareControlTransfer(frame, stack, false);
                        frame.jumpTo(instruction.labelTrue());
                        break;
                    case BR_IF:
                        BR_IF(frame, stack, instruction);
                        break;
                    case BR_TABLE:
                        BR_TABLE(frame, stack, instruction);
                        break;
                    case RETURN:
                        shouldReturn = true;
                        break;
                    case CALL_INDIRECT:
                        CALL_INDIRECT(stack, instance, callStack, operands);
                        break;
                    case DROP:
                        stack.pop();
                        break;
                    case SELECT:
                        SELECT(stack);
                        break;
                    case SELECT_T:
                        SELECT_T(stack, operands);
                        break;
                    case END:
                        {
                            if (frame.doControlTransfer && frame.isControlFrame) {
                                doControlTransfer(instance, stack, frame, instruction.scope());
                            } else {
                                frame.endOfNonControlBlock();
                            }

                            // if this is the last end, then we're done with
                            // the function
                            if (frame.isLastBlock()) {
                                break loop;
                            }
                            break;
                        }
                    case LOCAL_GET:
                        stack.push(frame.local((int) operands[0]));
                        break;
                    case LOCAL_SET:
                        frame.setLocal((int) operands[0], stack.pop());
                        break;
                    case LOCAL_TEE:
                        // here we peek instead of pop, leaving it on the stack
                        frame.setLocal((int) operands[0], stack.peek());
                        break;
                    case GLOBAL_GET:
                        GLOBAL_GET(stack, instance, operands);
                        break;
                    case GLOBAL_SET:
                        GLOBAL_SET(stack, instance, operands);
                        break;
                    case TABLE_GET:
                        TABLE_GET(stack, instance, operands);
                        break;
                    case TABLE_SET:
                        TABLE_SET(stack, instance, operands);
                        break;
                        // TODO signed and unsigned are the same right now
                    case I32_LOAD:
                        I32_LOAD(stack, instance, operands);
                        break;
                    case I64_LOAD:
                        I64_LOAD(stack, instance, operands);
                        break;
                    case F32_LOAD:
                        F32_LOAD(stack, instance, operands);
                        break;
                    case F64_LOAD:
                        F64_LOAD(stack, instance, operands);
                        break;
                    case I32_LOAD8_S:
                        I32_LOAD8_S(stack, instance, operands);
                        break;
                    case I64_LOAD8_S:
                        I64_LOAD8_S(stack, instance, operands);
                        break;
                    case I32_LOAD8_U:
                        I32_LOAD8_U(stack, instance, operands);
                        break;
                    case I64_LOAD8_U:
                        I64_LOAD8_U(stack, instance, operands);
                        break;
                    case I32_LOAD16_S:
                        I32_LOAD16_S(stack, instance, operands);
                        break;
                    case I64_LOAD16_S:
                        I64_LOAD16_S(stack, instance, operands);
                        break;
                    case I32_LOAD16_U:
                        I32_LOAD16_U(stack, instance, operands);
                        break;
                    case I64_LOAD16_U:
                        I64_LOAD16_U(stack, instance, operands);
                        break;
                    case I64_LOAD32_S:
                        I64_LOAD32_S(stack, instance, operands);
                        break;
                    case I64_LOAD32_U:
                        I64_LOAD32_U(stack, instance, operands);
                        break;
                    case I32_STORE:
                        I32_STORE(stack, instance, operands);
                        break;
                    case I32_STORE16:
                    case I64_STORE16:
                        I64_STORE16(stack, instance, operands);
                        break;
                    case I64_STORE:
                        I64_STORE(stack, instance, operands);
                        break;
                    case F32_STORE:
                        F32_STORE(stack, instance, operands);
                        break;
                    case F64_STORE:
                        F64_STORE(stack, instance, operands);
                        break;
                    case MEMORY_GROW:
                        MEMORY_GROW(stack, instance);
                        break;
                    case MEMORY_FILL:
                        MEMORY_FILL(stack, instance, operands);
                        break;
                    case I32_STORE8:
                    case I64_STORE8:
                        I64_STORE8(stack, instance, operands);
                        break;
                    case I64_STORE32:
                        I64_STORE32(stack, instance, operands);
                        break;
                    case MEMORY_SIZE:
                        MEMORY_SIZE(stack, instance);
                        break;
                        // TODO 32bit and 64 bit operations are the same for now
                    case I32_CONST:
                        stack.push(Value.i32(operands[0]));
                        break;
                    case I64_CONST:
                        stack.push(Value.i64(operands[0]));
                        break;
                    case F32_CONST:
                        stack.push(Value.f32(operands[0]));
                        break;
                    case F64_CONST:
                        stack.push(Value.f64(operands[0]));
                        break;
                    case I32_EQ:
                        I32_EQ(stack);
                        break;
                    case I64_EQ:
                        I64_EQ(stack);
                        break;
                    case I32_NE:
                        I32_NE(stack);
                        break;
                    case I64_NE:
                        I64_NE(stack);
                        break;
                    case I32_EQZ:
                        I32_EQZ(stack);
                        break;
                    case I64_EQZ:
                        I64_EQZ(stack);
                        break;
                    case I32_LT_S:
                        I32_LT_S(stack);
                        break;
                    case I32_LT_U:
                        I32_LT_U(stack);
                        break;
                    case I64_LT_S:
                        I64_LT_S(stack);
                        break;
                    case I64_LT_U:
                        I64_LT_U(stack);
                        break;
                    case I32_GT_S:
                        I32_GT_S(stack);
                        break;
                    case I32_GT_U:
                        I32_GT_U(stack);
                        break;
                    case I64_GT_S:
                        I64_GT_S(stack);
                        break;
                    case I64_GT_U:
                        I64_GT_U(stack);
                        break;
                    case I32_GE_S:
                        I32_GE_S(stack);
                        break;
                    case I32_GE_U:
                        I32_GE_U(stack);
                        break;
                    case I64_GE_U:
                        I64_GE_U(stack);
                        break;
                    case I64_GE_S:
                        I64_GE_S(stack);
                        break;
                    case I32_LE_S:
                        I32_LE_S(stack);
                        break;
                    case I32_LE_U:
                        I32_LE_U(stack);
                        break;
                    case I64_LE_S:
                        I64_LE_S(stack);
                        break;
                    case I64_LE_U:
                        I64_LE_U(stack);
                        break;
                    case F32_EQ:
                        F32_EQ(stack);
                        break;
                    case F64_EQ:
                        F64_EQ(stack);
                        break;
                    case I32_CLZ:
                        I32_CLZ(stack);
                        break;
                    case I32_CTZ:
                        I32_CTZ(stack);
                        break;
                    case I32_POPCNT:
                        I32_POPCNT(stack);
                        break;
                    case I32_ADD:
                        I32_ADD(stack);
                        break;
                    case I64_ADD:
                        I64_ADD(stack);
                        break;
                    case I32_SUB:
                        I32_SUB(stack);
                        break;
                    case I64_SUB:
                        I64_SUB(stack);
                        break;
                    case I32_MUL:
                        I32_MUL(stack);
                        break;
                    case I64_MUL:
                        I64_MUL(stack);
                        break;
                    case I32_DIV_S:
                        I32_DIV_S(stack);
                        break;
                    case I32_DIV_U:
                        I32_DIV_U(stack);
                        break;
                    case I64_DIV_S:
                        I64_DIV_S(stack);
                        break;
                    case I64_DIV_U:
                        I64_DIV_U(stack);
                        break;
                    case I32_REM_S:
                        I32_REM_S(stack);
                        break;
                    case I32_REM_U:
                        I32_REM_U(stack);
                        break;
                    case I64_AND:
                        I64_AND(stack);
                        break;
                    case I64_OR:
                        I64_OR(stack);
                        break;
                    case I64_XOR:
                        I64_XOR(stack);
                        break;
                    case I64_SHL:
                        I64_SHL(stack);
                        break;
                    case I64_SHR_S:
                        I64_SHR_S(stack);
                        break;
                    case I64_SHR_U:
                        I64_SHR_U(stack);
                        break;
                    case I64_REM_S:
                        I64_REM_S(stack);
                        break;
                    case I64_REM_U:
                        I64_REM_U(stack);
                        break;
                    case I64_ROTL:
                        I64_ROTL(stack);
                        break;
                    case I64_ROTR:
                        I64_ROTR(stack);
                        break;
                    case I64_CLZ:
                        I64_CLZ(stack);
                        break;
                    case I64_CTZ:
                        I64_CTZ(stack);
                        break;
                    case I64_POPCNT:
                        I64_POPCNT(stack);
                        break;
                    case F32_NEG:
                        F32_NEG(stack);
                        break;
                    case F64_NEG:
                        F64_NEG(stack);
                        break;
                    case CALL:
                        CALL(stack, instance, callStack, operands);
                        break;
                    case I32_AND:
                        I32_AND(stack);
                        break;
                    case I32_OR:
                        I32_OR(stack);
                        break;
                    case I32_XOR:
                        I32_XOR(stack);
                        break;
                    case I32_SHL:
                        I32_SHL(stack);
                        break;
                    case I32_SHR_S:
                        I32_SHR_S(stack);
                        break;
                    case I32_SHR_U:
                        I32_SHR_U(stack);
                        break;
                    case I32_ROTL:
                        I32_ROTL(stack);
                        break;
                    case I32_ROTR:
                        I32_ROTR(stack);
                        break;
                    case F32_ADD:
                        F32_ADD(stack);
                        break;
                    case F64_ADD:
                        F64_ADD(stack);
                        break;
                    case F32_SUB:
                        F32_SUB(stack);
                        break;
                    case F64_SUB:
                        F64_SUB(stack);
                        break;
                    case F32_MUL:
                        F32_MUL(stack);
                        break;
                    case F64_MUL:
                        F64_MUL(stack);
                        break;
                    case F32_DIV:
                        F32_DIV(stack);
                        break;
                    case F64_DIV:
                        F64_DIV(stack);
                        break;
                    case F32_MIN:
                        F32_MIN(stack);
                        break;
                    case F64_MIN:
                        F64_MIN(stack);
                        break;
                    case F32_MAX:
                        F32_MAX(stack);
                        break;
                    case F64_MAX:
                        F64_MAX(stack);
                        break;
                    case F32_SQRT:
                        F32_SQRT(stack);
                        break;
                    case F64_SQRT:
                        F64_SQRT(stack);
                        break;
                    case F32_FLOOR:
                        F32_FLOOR(stack);
                        break;
                    case F64_FLOOR:
                        F64_FLOOR(stack);
                        break;
                    case F32_CEIL:
                        F32_CEIL(stack);
                        break;
                    case F64_CEIL:
                        F64_CEIL(stack);
                        break;
                    case F32_TRUNC:
                        F32_TRUNC(stack);
                        break;
                    case F64_TRUNC:
                        F64_TRUNC(stack);
                        break;
                    case F32_NEAREST:
                        F32_NEAREST(stack);
                        break;
                    case F64_NEAREST:
                        F64_NEAREST(stack);
                        break;
                        // For the extend_* operations, note that java
                        // automatically does this when casting from
                        // smaller to larger primitives
                    case I32_EXTEND_8_S:
                        I32_EXTEND_8_S(stack);
                        break;
                    case I32_EXTEND_16_S:
                        I32_EXTEND_16_S(stack);
                        break;
                    case I64_EXTEND_8_S:
                        I64_EXTEND_8_S(stack);
                        break;
                    case I64_EXTEND_16_S:
                        I64_EXTEND_16_S(stack);
                        break;
                    case I64_EXTEND_32_S:
                        I64_EXTEND_32_S(stack);
                        break;
                    case F64_CONVERT_I64_U:
                        F64_CONVERT_I64_U(stack);
                        break;
                    case F64_CONVERT_I32_U:
                        F64_CONVERT_I32_U(stack);
                        break;
                    case F64_CONVERT_I32_S:
                        F64_CONVERT_I32_S(stack);
                        break;
                    case F64_PROMOTE_F32:
                        F64_PROMOTE_F32(stack);
                        break;
                    case F64_REINTERPRET_I64:
                        F64_REINTERPRET_I64(stack);
                        break;
                    case I64_TRUNC_F64_S:
                        I64_TRUNC_F64_S(stack);
                        break;
                    case I32_WRAP_I64:
                        I32_WRAP_I64(stack);
                        break;
                    case I64_EXTEND_I32_S:
                        I64_EXTEND_I32_S(stack);
                        break;
                    case I64_EXTEND_I32_U:
                        I64_EXTEND_I32_U(stack);
                        break;
                    case I32_REINTERPRET_F32:
                        I32_REINTERPRET_F32(stack);
                        break;
                    case I64_REINTERPRET_F64:
                        I64_REINTERPRET_F64(stack);
                        break;
                    case F32_REINTERPRET_I32:
                        F32_REINTERPRET_I32(stack);
                        break;
                    case F32_COPYSIGN:
                        F32_COPYSIGN(stack);
                        break;
                    case F32_ABS:
                        F32_ABS(stack);
                        break;
                    case F64_COPYSIGN:
                        F64_COPYSIGN(stack);
                        break;
                    case F64_ABS:
                        F64_ABS(stack);
                        break;
                    case F32_NE:
                        F32_NE(stack);
                        break;
                    case F64_NE:
                        F64_NE(stack);
                        break;
                    case F32_LT:
                        F32_LT(stack);
                        break;
                    case F64_LT:
                        F64_LT(stack);
                        break;
                    case F32_LE:
                        F32_LE(stack);
                        break;
                    case F64_LE:
                        F64_LE(stack);
                        break;
                    case F32_GE:
                        F32_GE(stack);
                        break;
                    case F64_GE:
                        F64_GE(stack);
                        break;
                    case F32_GT:
                        F32_GT(stack);
                        break;
                    case F64_GT:
                        F64_GT(stack);
                        break;
                    case F32_DEMOTE_F64:
                        F32_DEMOTE_F64(stack);
                        break;
                    case F32_CONVERT_I32_S:
                        F32_CONVERT_I32_S(stack);
                        break;
                    case I32_TRUNC_F32_S:
                        I32_TRUNC_F32_S(stack);
                        break;
                    case I32_TRUNC_SAT_F32_S:
                        I32_TRUNC_SAT_F32_S(stack);
                        break;
                    case I32_TRUNC_SAT_F32_U:
                        I32_TRUNC_SAT_F32_U(stack);
                        break;
                    case I32_TRUNC_SAT_F64_S:
                        I32_TRUNC_SAY_F64_S(stack);
                        break;
                    case I32_TRUNC_SAT_F64_U:
                        I32_TRUNC_SAT_F64_U(stack);
                        break;
                    case F32_CONVERT_I32_U:
                        F32_CONVERT_I32_U(stack);
                        break;
                    case I32_TRUNC_F32_U:
                        I32_TRUNC_F32_U(stack);
                        break;
                    case F32_CONVERT_I64_S:
                        F32_CONVERT_I64_S(stack);
                        break;
                    case F32_CONVERT_I64_U:
                        F32_CONVERT_I64_U(stack);
                        break;
                    case F64_CONVERT_I64_S:
                        F64_CONVERT_I64_S(stack);
                        break;
                    case I64_TRUNC_F32_U:
                        I64_TRUNC_F32_U(stack);
                        break;
                    case I64_TRUNC_F64_U:
                        I64_TRUNC_F64_U(stack);
                        break;
                    case I64_TRUNC_SAT_F32_S:
                        I64_TRUNC_SAT_F32_S(stack);
                        break;
                    case I64_TRUNC_SAT_F32_U:
                        I64_TRUNC_SAT_F32_U(stack);
                        break;
                    case I64_TRUNC_SAT_F64_S:
                        I64_TRUNC_SAT_F64_S(stack);
                        break;
                    case I64_TRUNC_SAT_F64_U:
                        I64_TRUNC_SAT_F64_U(stack);
                        break;
                    case I32_TRUNC_F64_S:
                        I32_TRUNC_F64_S(stack);
                        break;
                    case I32_TRUNC_F64_U:
                        I32_TRUNC_F64_U(stack);
                        break;
                    case I64_TRUNC_F32_S:
                        I64_TRUNC_F32_S(stack);
                        break;
                    case MEMORY_INIT:
                        MEMORY_INIT(stack, instance, operands);
                        break;
                    case TABLE_INIT:
                        TABLE_INIT(stack, instance, operands);
                        break;
                    case DATA_DROP:
                        DATA_DROP(instance, operands);
                        break;
                    case MEMORY_COPY:
                        MEMORY_COPY(stack, instance, operands);
                        break;
                    case TABLE_COPY:
                        TABLE_COPY(stack, instance, operands);
                        break;
                    case TABLE_FILL:
                        TABLE_FILL(stack, instance, operands);
                        break;
                    case TABLE_SIZE:
                        TABLE_SIZE(stack, instance, operands);
                        break;
                    case TABLE_GROW:
                        TABLE_GROW(stack, instance, operands);
                        break;
                    case REF_FUNC:
                        stack.push(Value.funcRef(operands[0]));
                        break;
                    case REF_NULL:
                        REF_NULL(stack, operands);
                        break;
                    case REF_IS_NULL:
                        REF_IS_NULL(stack);
                        break;
                    case ELEM_DROP:
                        ELEM_DROP(instance, operands);
                        break;
                    default:
                        throw new RuntimeException(
                                "Machine doesn't recognize Instruction " + instruction);
                }
            }
        } catch (ChicoryException e) {
            // propagate ChicoryExceptions
            throw e;
        } catch (ArithmeticException e) {
            if (e.getMessage().equalsIgnoreCase("/ by zero")
                    || e.getMessage()
                            .contains("divide by zero")) { // On Linux i64 throws "BigInteger divide
                // by zero"
                throw new WASMRuntimeException("integer divide by zero: " + e.getMessage(), e);
            }
            throw new WASMRuntimeException(e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("undefined element " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        }
    }

    private static void I32_GE_U(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(Integer.compareUnsigned(a, b) >= 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I64_GT_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Long.compareUnsigned(a, b) > 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I32_GE_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(a >= b ? Value.TRUE : Value.FALSE);
    }

    private static void I64_GE_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Long.compareUnsigned(a, b) >= 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I64_GE_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(a >= b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_LE_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(a <= b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_LE_U(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(Integer.compareUnsigned(a, b) <= 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I64_LE_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(a <= b ? Value.TRUE : Value.FALSE);
    }

    private static void I64_LE_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Long.compareUnsigned(a, b) <= 0 ? Value.TRUE : Value.FALSE);
    }

    private static void F32_EQ(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(a == b ? Value.TRUE : Value.FALSE);
    }

    private static void F64_EQ(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(a == b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_CLZ(MStack stack) {
        var tos = stack.pop().asInt();
        var count = Integer.numberOfLeadingZeros(tos);
        stack.push(Value.i32(count));
    }

    private static void I32_CTZ(MStack stack) {
        var tos = stack.pop().asInt();
        var count = Integer.numberOfTrailingZeros(tos);
        stack.push(Value.i32(count));
    }

    private static void I32_POPCNT(MStack stack) {
        var tos = stack.pop().asInt();
        var count = Integer.bitCount(tos);
        stack.push(Value.i32(count));
    }

    private static void I32_ADD(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(a + b));
    }

    private static void I64_ADD(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(a + b));
    }

    private static void I32_SUB(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(b - a));
    }

    private static void I64_SUB(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(b - a));
    }

    private static void I32_MUL(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(a * b));
    }

    private static void I64_MUL(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(a * b));
    }

    private static void I32_DIV_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new WASMRuntimeException("integer overflow");
        }
        stack.push(Value.i32(a / b));
    }

    private static void I32_DIV_U(MStack stack) {
        var b = stack.pop().asUInt();
        var a = stack.pop().asUInt();
        stack.push(Value.i32(a / b));
    }

    private static void I64_EXTEND_8_S(MStack stack) {
        var tos = stack.pop().asByte();
        stack.push(Value.i64(tos));
    }

    private static void I64_EXTEND_16_S(MStack stack) {
        var tos = stack.pop().asShort();
        stack.push(Value.i64(tos));
    }

    private static void I64_EXTEND_32_S(MStack stack) {
        var tos = stack.pop().asInt();
        stack.push(Value.i64(tos));
    }

    private static void F64_CONVERT_I64_U(MStack stack) {
        var tos = stack.pop().asLong();
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
        stack.push(Value.f64(Double.doubleToLongBits(d)));
    }

    private static void F64_CONVERT_I32_U(MStack stack) {
        long tos = stack.pop().asUInt();
        stack.push(Value.f64(Double.doubleToLongBits(tos)));
    }

    private static void F64_CONVERT_I32_S(MStack stack) {
        var tos = stack.pop().asInt();
        stack.push(Value.fromDouble(tos));
    }

    private static void I32_EXTEND_8_S(MStack stack) {
        var tos = stack.pop().asByte();
        stack.push(Value.i32(tos));
    }

    private static void F64_NEAREST(MStack stack) {
        var val = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.rint(val)));
    }

    private static void F32_NEAREST(MStack stack) {
        var val = stack.pop().asFloat();
        stack.push(Value.fromFloat((float) Math.rint(val)));
    }

    private static void F64_TRUNC(MStack stack) {
        var val = stack.pop().asDouble();
        stack.push(Value.fromDouble((val < 0) ? Math.ceil(val) : Math.floor(val)));
    }

    private static void F64_CEIL(MStack stack) {
        var val = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.ceil(val)));
    }

    private static void F32_CEIL(MStack stack) {
        var val = stack.pop().asFloat();
        stack.push(Value.fromFloat((float) Math.ceil(val)));
    }

    private static void F64_FLOOR(MStack stack) {
        var val = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.floor(val)));
    }

    private static void F32_FLOOR(MStack stack) {
        var val = stack.pop().asFloat();
        stack.push(Value.fromFloat((float) Math.floor(val)));
    }

    private static void F64_SQRT(MStack stack) {
        var val = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.sqrt(val)));
    }

    private static void F32_SQRT(MStack stack) {
        var val = stack.pop().asFloat();
        stack.push(Value.fromFloat((float) Math.sqrt(val)));
    }

    private static void F64_MAX(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.max(a, b)));
    }

    private static void F32_MAX(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(Math.max(a, b)));
    }

    private static void F64_MIN(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(Math.min(a, b)));
    }

    private static void F32_MIN(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(Math.min(a, b)));
    }

    private static void F64_DIV(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(b / a));
    }

    private static void F32_DIV(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(b / a));
    }

    private static void F64_MUL(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(b * a));
    }

    private static void F32_MUL(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(b * a));
    }

    private static void F64_SUB(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(b - a));
    }

    private static void F32_SUB(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(b - a));
    }

    private static void F64_ADD(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();
        stack.push(Value.fromDouble(a + b));
    }

    private static void F32_ADD(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();
        stack.push(Value.fromFloat(a + b));
    }

    private static void I32_ROTR(MStack stack) {
        var c = stack.pop().asInt();
        var v = stack.pop().asInt();
        var z = (v >>> c) | (v << (32 - c));
        stack.push(Value.i32(z));
    }

    private static void I32_ROTL(MStack stack) {
        var c = stack.pop().asInt();
        var v = stack.pop().asInt();
        var z = (v << c) | (v >>> (32 - c));
        stack.push(Value.i32(z));
    }

    private static void I32_SHR_U(MStack stack) {
        var c = stack.pop().asInt();
        var v = stack.pop().asInt();
        stack.push(Value.i32(v >>> c));
    }

    private static void I32_SHR_S(MStack stack) {
        var c = stack.pop().asInt();
        var v = stack.pop().asInt();
        stack.push(Value.i32(v >> c));
    }

    private static void I32_SHL(MStack stack) {
        var c = stack.pop().asInt();
        var v = stack.pop().asInt();
        stack.push(Value.i32(v << c));
    }

    private static void I32_XOR(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(a ^ b));
    }

    private static void I32_OR(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(a | b));
    }

    private static void I32_AND(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(Value.i32(a & b));
    }

    private static void I64_POPCNT(MStack stack) {
        var tos = stack.pop().asLong();
        var count = Long.bitCount(tos);
        stack.push(Value.i64(count));
    }

    private static void I64_CTZ(MStack stack) {
        var tos = stack.pop();
        var count = Long.numberOfTrailingZeros(tos.asLong());
        stack.push(Value.i64(count));
    }

    private static void I64_CLZ(MStack stack) {
        var tos = stack.pop();
        var count = Long.numberOfLeadingZeros(tos.asLong());
        stack.push(Value.i64(count));
    }

    private static void I64_ROTR(MStack stack) {
        var c = stack.pop().asLong();
        var v = stack.pop().asLong();
        var z = (v >>> c) | (v << (64 - c));
        stack.push(Value.i64(z));
    }

    private static void I64_ROTL(MStack stack) {
        var c = stack.pop().asLong();
        var v = stack.pop().asLong();
        var z = (v << c) | (v >>> (64 - c));
        stack.push(Value.i64(z));
    }

    private static void I64_REM_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Value.i64(Long.remainderUnsigned(a, b)));
    }

    private static void I64_REM_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Value.i64(a % b));
    }

    private static void I64_SHR_U(MStack stack) {
        var c = stack.pop().asLong();
        var v = stack.pop().asLong();
        stack.push(Value.i64(v >>> c));
    }

    private static void I64_SHR_S(MStack stack) {
        var c = stack.pop().asLong();
        var v = stack.pop().asLong();
        stack.push(Value.i64(v >> c));
    }

    private static void I64_SHL(MStack stack) {
        var c = stack.pop().asLong();
        var v = stack.pop().asLong();
        stack.push(Value.i64(v << c));
    }

    private static void I64_XOR(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(a ^ b));
    }

    private static void I64_OR(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(a | b));
    }

    private static void I64_AND(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(Value.i64(a & b));
    }

    private static void I32_REM_U(MStack stack) {
        var b = stack.pop().asUInt();
        var a = stack.pop().asUInt();
        stack.push(Value.i32(a % b));
    }

    private static void I32_REM_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(Value.i32(a % b));
    }

    private static void I64_DIV_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Value.i64(Long.divideUnsigned(a, b)));
    }

    private static void I64_DIV_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        if (a == Long.MIN_VALUE && b == -1L) {
            throw new WASMRuntimeException("integer overflow");
        }
        stack.push(Value.i64(a / b));
    }

    private static void I64_GT_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(a > b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_GT_U(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(Integer.compareUnsigned(a, b) > 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I32_GT_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(a > b ? Value.TRUE : Value.FALSE);
    }

    private static void I64_LT_U(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(Long.compareUnsigned(a, b) < 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I64_LT_S(MStack stack) {
        var b = stack.pop().asLong();
        var a = stack.pop().asLong();
        stack.push(a < b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_LT_U(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(Integer.compareUnsigned(a, b) < 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I32_LT_S(MStack stack) {
        var b = stack.pop().asInt();
        var a = stack.pop().asInt();
        stack.push(a < b ? Value.TRUE : Value.FALSE);
    }

    private static void I64_EQZ(MStack stack) {
        var a = stack.pop().asLong();
        stack.push(a == 0L ? Value.TRUE : Value.FALSE);
    }

    private static void I32_EQZ(MStack stack) {
        var a = stack.pop().asInt();
        stack.push(a == 0 ? Value.TRUE : Value.FALSE);
    }

    private static void I64_NE(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(a == b ? Value.FALSE : Value.TRUE);
    }

    private static void I32_NE(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(a == b ? Value.FALSE : Value.TRUE);
    }

    private static void I64_EQ(MStack stack) {
        var a = stack.pop().asLong();
        var b = stack.pop().asLong();
        stack.push(a == b ? Value.TRUE : Value.FALSE);
    }

    private static void I32_EQ(MStack stack) {
        var a = stack.pop().asInt();
        var b = stack.pop().asInt();
        stack.push(a == b ? Value.TRUE : Value.FALSE);
    }

    private static void MEMORY_SIZE(MStack stack, Instance instance) {
        var sz = instance.memory().pages();
        stack.push(Value.i32(sz));
    }

    private static void I64_STORE32(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asLong();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeI32(ptr, (int) value);
    }

    private static void I64_STORE8(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asByte();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeByte(ptr, value);
    }

    private static void F64_PROMOTE_F32(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.fromDouble(tos.asFloat()));
    }

    private static void F64_REINTERPRET_I64(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.f64(tos.asLong()));
    }

    private static void I32_WRAP_I64(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.i32(tos.asInt()));
    }

    private static void I64_EXTEND_I32_S(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.i64(tos.asInt()));
    }

    private static void I64_EXTEND_I32_U(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.i64(tos.asUInt()));
    }

    private static void I32_REINTERPRET_F32(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.i32(tos.asInt()));
    }

    private static void I64_REINTERPRET_F64(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.i64(tos.asLong()));
    }

    private static void F32_REINTERPRET_I32(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.f32(tos.asInt()));
    }

    private static void F32_DEMOTE_F64(MStack stack) {
        var val = stack.pop().asDouble();

        stack.push(Value.fromFloat((float) val));
    }

    private static void F32_CONVERT_I32_S(MStack stack) {
        var tos = stack.pop().asInt();
        stack.push(Value.fromFloat((float) tos));
    }

    private static void I32_EXTEND_16_S(MStack stack) {
        var original = stack.pop().asInt() & 0xFFFF;
        if ((original & 0x8000) != 0) original |= 0xFFFF0000;
        stack.push(Value.i32(original & 0xFFFFFFFFL));
    }

    private static void I64_TRUNC_F64_S(MStack stack) {
        double tos = stack.pop().asDouble();

        if (Double.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        long tosL = (long) tos;
        if (tos == (double) Long.MIN_VALUE) {
            tosL = Long.MIN_VALUE;
        } else if (tosL == Long.MIN_VALUE || tosL == Long.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        stack.push(Value.i64(tosL));
    }

    private static void F32_COPYSIGN(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        if (a == 0xFFC00000L) { // +NaN
            stack.push(Value.fromFloat(Math.copySign(b, -1)));
        } else if (a == 0x7FC00000L) { // -NaN
            stack.push(Value.fromFloat(Math.copySign(b, +1)));
        } else {
            stack.push(Value.fromFloat(Math.copySign(b, a)));
        }
    }

    private static void F32_ABS(MStack stack) {
        var val = stack.pop().asFloat();

        stack.push(Value.fromFloat(Math.abs(val)));
    }

    private static void F64_ABS(MStack stack) {
        var val = stack.pop().asDouble();

        stack.push(Value.fromDouble(Math.abs(val)));
    }

    private static void F32_NE(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        stack.push(a == b ? Value.FALSE : Value.TRUE);
    }

    private static void F64_NE(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        stack.push(a == b ? Value.FALSE : Value.TRUE);
    }

    private static void F32_LT(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        stack.push(a > b ? Value.TRUE : Value.FALSE);
    }

    private static void F64_LT(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        stack.push(a > b ? Value.TRUE : Value.FALSE);
    }

    private static void F32_LE(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        stack.push(a >= b ? Value.TRUE : Value.FALSE);
    }

    private static void F64_LE(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        stack.push(a >= b ? Value.TRUE : Value.FALSE);
    }

    private static void F32_GE(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        stack.push(a <= b ? Value.TRUE : Value.FALSE);
    }

    private static void F64_GE(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        stack.push(a <= b ? Value.TRUE : Value.FALSE);
    }

    private static void F32_GT(MStack stack) {
        var a = stack.pop().asFloat();
        var b = stack.pop().asFloat();

        stack.push(a < b ? Value.TRUE : Value.FALSE);
    }

    private static void F64_GT(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        stack.push(a < b ? Value.TRUE : Value.FALSE);
    }

    private static void F32_CONVERT_I32_U(MStack stack) {
        var tos = stack.pop().asUInt();

        stack.push(Value.fromFloat((float) tos));
    }

    private static void F32_CONVERT_I64_S(MStack stack) {
        var tos = stack.pop().asLong();

        stack.push(Value.fromFloat((float) tos));
    }

    private static void REF_NULL(MStack stack, long[] operands) {
        var type = ValueType.forId((int) operands[0]);
        stack.push(new Value(type, (long) REF_NULL_VALUE));
    }

    private static void ELEM_DROP(Instance instance, long[] operands) {
        var x = (int) operands[0];
        instance.setElement(x, null);
    }

    private static void REF_IS_NULL(MStack stack) {
        var val = stack.pop();
        stack.push(
                val.equals(Value.EXTREF_NULL) || val.equals(Value.FUNCREF_NULL)
                        ? Value.TRUE
                        : Value.FALSE);
    }

    private static void DATA_DROP(Instance instance, long[] operands) {
        var segment = (int) operands[0];
        instance.memory().drop(segment);
    }

    private static void F64_CONVERT_I64_S(MStack stack) {
        var tos = stack.pop().asLong();

        stack.push(Value.fromDouble((double) tos));
    }

    private static void TABLE_GROW(MStack stack, Instance instance, long[] operands) {
        var tableidx = (int) operands[0];
        var table = instance.table(tableidx);

        var size = stack.pop().asInt();
        var valValue = stack.pop();
        var val = valValue.asExtRef();

        var res = table.grow(size, val, instance);
        stack.push(Value.i32(res));
    }

    private static void TABLE_SIZE(MStack stack, Instance instance, long[] operands) {
        var tableidx = (int) operands[0];
        var table = instance.table(tableidx);

        stack.push(Value.i32(table.size()));
    }

    private static void TABLE_FILL(MStack stack, Instance instance, long[] operands) {
        var tableidx = (int) operands[0];

        var size = stack.pop().asInt();
        var val = stack.pop().asExtRef();
        var offset = stack.pop().asInt();
        var end = offset + size;

        var table = instance.table(tableidx);

        if (size < 0 || end > table.size()) {
            throw new WASMRuntimeException("out of bounds table access");
        }

        for (int i = offset; i < end; i++) {
            table.setRef(i, val, instance);
        }
    }

    private static void TABLE_COPY(MStack stack, Instance instance, long[] operands) {
        var tableidxSrc = (int) operands[1];
        var tableidxDst = (int) operands[0];

        var size = stack.pop().asInt();
        var s = stack.pop().asInt();
        var d = stack.pop().asInt();
        var src = instance.table(tableidxSrc);
        var dest = instance.table(tableidxDst);

        if (size < 0 || (s < 0 || (size + s) > src.size()) || (d < 0 || (size + d) > dest.size())) {
            throw new WASMRuntimeException("out of bounds table access");
        }

        for (int i = size - 1; i >= 0; i--) {
            if (d <= s) {
                var val = src.ref(s++);
                var inst = src.instance(d);
                dest.setRef(d++, val.asFuncRef(), inst);
            } else {
                var val = src.ref(s + i);
                var inst = src.instance(d + i);
                dest.setRef(d + i, val.asFuncRef(), inst);
            }
        }
    }

    private static void MEMORY_COPY(MStack stack, Instance instance, long[] operands) {
        var memidxSrc = (int) operands[0];
        var memidxDst = (int) operands[1];
        if (memidxDst != 0 && memidxSrc != 0)
            throw new WASMRuntimeException(
                    "We don't support non zero index for memory: " + memidxSrc + " " + memidxDst);
        var size = stack.pop().asInt();
        var offset = stack.pop().asInt();
        var destination = stack.pop().asInt();
        instance.memory().copy(destination, offset, size);
    }

    private static void TABLE_INIT(MStack stack, Instance instance, long[] operands) {
        var tableidx = (int) operands[1];
        var elementidx = (int) operands[0];

        var size = stack.pop().asInt();
        var elemidx = stack.pop().asInt();
        var offset = stack.pop().asInt();
        var end = offset + size;

        var table = instance.table(tableidx);

        var elementCount = instance.elementCount();
        var currentElement = instance.element(elementidx);
        var currentElementCount =
                (currentElement instanceof PassiveElement) ? currentElement.elementCount() : 0;
        boolean isOutOfBounds =
                (size < 0
                        || elementidx > elementCount
                        || (size > 0
                                && (currentElement == null
                                        || !(currentElement instanceof PassiveElement)))
                        || elemidx + size > currentElementCount
                        || end > table.size());

        if (isOutOfBounds) {
            throw new WASMRuntimeException("out of bounds table access");
        } else if (size == 0) {
            return;
        }

        for (int i = offset; i < end; i++) {
            var val = getRuntimeElementValue(instance, elementidx, elemidx++);
            if (table.elementType() == ValueType.FuncRef) {
                if (val.asFuncRef() > instance.functionCount()) {
                    throw new WASMRuntimeException("out of bounds table access");
                }
                table.setRef(i, val.asFuncRef(), instance);
            } else {
                assert table.elementType() == ValueType.ExternRef;
                table.setRef(i, val.asExtRef(), instance);
            }
        }
    }

    private static void MEMORY_INIT(MStack stack, Instance instance, long[] operands) {
        var segmentId = (int) operands[0];
        var memidx = (int) operands[1];
        if (memidx != 0)
            throw new WASMRuntimeException("We don't support non zero index for memory: " + memidx);
        var size = stack.pop().asInt();
        var offset = stack.pop().asInt();
        var destination = stack.pop().asInt();
        instance.memory().initPassiveSegment(segmentId, destination, offset, size);
    }

    private static void I64_TRUNC_F32_S(MStack stack) {
        var tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        if (tos < Long.MIN_VALUE || tos >= Long.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        stack.push(Value.i64((long) tos));
    }

    private static void I32_TRUNC_F64_U(MStack stack) {
        double tos = stack.pop().asDouble();
        if (Double.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        var tosL = (long) tos;
        if (tosL < 0 || tosL > 0xFFFFFFFFL) {
            throw new WASMRuntimeException("integer overflow");
        }
        stack.push(Value.i32(tosL & 0xFFFFFFFFL));
    }

    private static void I32_TRUNC_F64_S(MStack stack) {
        var tos = stack.pop().asDouble();

        if (Double.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        var tosL = (long) tos;
        if (tosL < Integer.MIN_VALUE || tosL > Integer.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        stack.push(Value.i32(tosL));
    }

    private static void I64_TRUNC_SAT_F64_U(MStack stack) {
        double tos = stack.pop().asDouble();

        long tosL;
        if (Double.isNaN(tos) || tos < 0) {
            tosL = 0L;
        } else if (tos > Math.pow(2, 64) - 1) {
            tosL = 0xFFFFFFFFFFFFFFFFL;
        } else {
            if (tos < Long.MAX_VALUE) {
                tosL = (long) tos;
            } else {
                // See I64_TRUNC_F32_U for notes on implementation. This is
                // the double-based equivalent of that.
                tosL = Long.MAX_VALUE + (long) (tos - (double) Long.MAX_VALUE) + 1;
                if (tosL >= 0) {
                    throw new WASMRuntimeException("integer overflow");
                }
            }
        }

        stack.push(Value.i64(tosL));
    }

    private static void I64_TRUNC_SAT_F64_S(MStack stack) {
        var tos = stack.pop().asDouble();

        if (Double.isNaN(tos)) {
            tos = 0;
        } else if (tos <= Long.MIN_VALUE) {
            tos = Long.MIN_VALUE;
        } else if (tos >= Long.MAX_VALUE) {
            tos = Long.MAX_VALUE;
        }

        stack.push(Value.i64((long) tos));
    }

    private static void I64_TRUNC_SAT_F32_U(MStack stack) {
        var tos = stack.pop().asFloat();

        long tosL;
        if (Float.isNaN(tos) || tos < 0) {
            tosL = 0L;
        } else if (tos > Math.pow(2, 64) - 1) {
            tosL = 0xFFFFFFFFFFFFFFFFL;
        } else {
            if (tos < Long.MAX_VALUE) {
                tosL = (long) tos;
            } else {
                // See I64_TRUNC_F32_U for notes on implementation. This is
                // the double-based equivalent of that.
                tosL = Long.MAX_VALUE + (long) (tos - (double) Long.MAX_VALUE) + 1;
                if (tosL >= 0) {
                    throw new WASMRuntimeException("integer overflow");
                }
            }
        }

        stack.push(Value.i64(tosL));
    }

    private static void I64_TRUNC_SAT_F32_S(MStack stack) {
        var tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            tos = 0;
        } else if (tos <= Long.MIN_VALUE) {
            tos = Long.MIN_VALUE;
        } else if (tos >= Long.MAX_VALUE) {
            tos = Long.MAX_VALUE;
        }

        stack.push(Value.i64((long) tos));
    }

    private static void I64_TRUNC_F64_U(MStack stack) {
        var tos = stack.pop().asDouble();

        if (Double.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        if (tos >= 2 * (double) Long.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        long tosL;
        if (tos < Long.MAX_VALUE) {
            tosL = (long) tos;
            if (tosL < 0) {
                throw new WASMRuntimeException("integer overflow");
            }
        } else {
            // See I64_TRUNC_F32_U for notes on implementation. This is
            // the double-based equivalent of that.
            tosL = Long.MAX_VALUE + (long) (tos - (double) Long.MAX_VALUE) + 1;
            if (tosL >= 0) {
                throw new WASMRuntimeException("integer overflow");
            }
        }

        stack.push(Value.i64(tosL));
    }

    private static void I64_TRUNC_F32_U(MStack stack) {
        var tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        if (tos >= 2 * (float) Long.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        long tosL;
        if (tos < Long.MAX_VALUE) {
            tosL = (long) tos;
            if (tosL < 0) {
                throw new WASMRuntimeException("integer overflow");
            }
        } else {
            // This works for getting the unsigned value because binary addition
            // yields the correct interpretation in both unsigned &
            // 2's-complement
            // no matter which the operands are considered to be.
            tosL = Long.MAX_VALUE + (long) (tos - (float) Long.MAX_VALUE) + 1;
            if (tosL >= 0) {
                // Java's comparison operators assume signed integers. In the
                // case
                // that we're in the range of unsigned values where the sign bit
                // is set, Java considers these values to be negative so we have
                // to check for >= 0 to detect overflow.
                throw new WASMRuntimeException("integer overflow");
            }
        }

        stack.push(Value.i64(tosL));
    }

    private static void F32_CONVERT_I64_U(MStack stack) {
        var tos = stack.pop().asLong();

        float f;
        if (tos >= 0) {
            f = tos;
        } else {
            // only preserve 24 bits of precision (plus one for rounding) to
            // avoid rounding errors (64 - 24 == 40)
            long sum = tos + 0xff_ffff_ffffL;
            // did the add overflow? add the MSB back on after the shift
            long shiftIn = ((sum ^ tos) & Long.MIN_VALUE) >>> 39;
            f = Math.scalb((float) ((sum >>> 40) | shiftIn), 40);
        }

        stack.push(Value.f32(Float.floatToIntBits(f)));
    }

    private static void I32_TRUNC_F32_U(MStack stack) {
        var tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        long tosL = (long) tos;
        if (tosL < 0 || tosL >= 0xFFFFFFFFL) {
            throw new WASMRuntimeException("integer overflow");
        }

        stack.push(Value.i32(tosL));
    }

    private static void I32_TRUNC_SAT_F64_U(MStack stack) {
        double tos = Double.longBitsToDouble(stack.pop().asLong());

        long tosL;
        if (Double.isNaN(tos) || tos < 0) {
            tosL = 0;
        } else if (tos > 0xFFFFFFFFL) {
            tosL = 0xFFFFFFFFL;
        } else {
            tosL = (long) tos;
        }
        stack.push(Value.i32(tosL));
    }

    private static void I32_TRUNC_SAY_F64_S(MStack stack) {
        var tos = stack.pop().asDouble();

        if (Double.isNaN(tos)) {
            tos = 0;
        } else if (tos <= Integer.MIN_VALUE) {
            tos = Integer.MIN_VALUE;
        } else if (tos >= Integer.MAX_VALUE) {
            tos = Integer.MAX_VALUE;
        }

        stack.push(Value.i32((int) tos));
    }

    private static void I32_TRUNC_SAT_F32_U(MStack stack) {
        var tos = stack.pop().asFloat();

        long tosL;
        if (Float.isNaN(tos) || tos < 0) {
            tosL = 0L;
        } else if (tos >= 0xFFFFFFFFL) {
            tosL = 0xFFFFFFFFL;
        } else {
            tosL = (long) tos;
        }

        stack.push(Value.i32(tosL));
    }

    private static void I32_TRUNC_SAT_F32_S(MStack stack) {
        var tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            tos = 0;
        } else if (tos < Integer.MIN_VALUE) {
            tos = Integer.MIN_VALUE;
        } else if (tos > Integer.MAX_VALUE) {
            tos = Integer.MAX_VALUE;
        }

        stack.push(Value.i32((int) tos));
    }

    private static void I32_TRUNC_F32_S(MStack stack) {
        float tos = stack.pop().asFloat();

        if (Float.isNaN(tos)) {
            throw new WASMRuntimeException("invalid conversion to integer");
        }

        if (tos < Integer.MIN_VALUE || tos >= Integer.MAX_VALUE) {
            throw new WASMRuntimeException("integer overflow");
        }

        stack.push(Value.i32((long) tos));
    }

    private static void F64_COPYSIGN(MStack stack) {
        var a = stack.pop().asDouble();
        var b = stack.pop().asDouble();

        if (a == 0xFFC0000000000000L) { // +NaN
            stack.push(Value.fromDouble(Math.copySign(b, -1)));
        } else if (a == 0x7FC0000000000000L) { // -NaN
            stack.push(Value.fromDouble(Math.copySign(b, +1)));
        } else {
            stack.push(Value.fromDouble(Math.copySign(b, a)));
        }
    }

    private static void F32_TRUNC(MStack stack) {
        var val = stack.pop().asFloat();
        stack.push(Value.fromFloat((float) ((val < 0) ? Math.ceil(val) : Math.floor(val))));
    }

    private static void CALL(
            MStack stack, Instance instance, ArrayDeque<StackFrame> callStack, long[] operands) {
        var funcId = (int) operands[0];
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);
        // given a list of param types, let's pop those params off the stack
        // and pass as args to the function call
        var args = extractArgsForParams(stack, type.params());
        call(stack, instance, callStack, funcId, args, type, false);
    }

    private static void F64_NEG(MStack stack) {
        var tos = stack.pop().asDouble();

        double result;
        if (Double.isNaN(tos)) {
            result = Double.longBitsToDouble(Double.doubleToRawLongBits(tos) ^ 0x8000000000000000L);
        } else {
            result = -1.0d * tos;
        }

        stack.push(Value.fromDouble(result));
    }

    private static void F32_NEG(MStack stack) {
        var tos = stack.pop().asFloat();

        float result;
        if (Float.isNaN(tos)) {
            result = Float.intBitsToFloat(Float.floatToRawIntBits(tos) ^ 0x80000000);
        } else {
            result = -1.0f * tos;
        }

        stack.push(Value.fromFloat(result));
    }

    private static void MEMORY_FILL(MStack stack, Instance instance, long[] operands) {
        var memidx = (int) operands[0];
        if (memidx != 0) {
            throw new WASMRuntimeException("We don't support multiple memories just yet");
        }
        var size = stack.pop().asInt();
        var val = stack.pop().asByte();
        var offset = stack.pop().asInt();
        var end = (size + offset);
        instance.memory().fill(val, offset, end);
    }

    private static void MEMORY_GROW(MStack stack, Instance instance) {
        var size = stack.pop().asInt();
        var nPages = instance.memory().grow(size);
        stack.push(Value.i32(nPages));
    }

    private static void F64_STORE(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asDouble();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeF64(ptr, value);
    }

    private static void F32_STORE(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asFloat();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeF32(ptr, value);
    }

    private static void I64_STORE(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asLong();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeLong(ptr, value);
    }

    private static void I64_STORE16(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asShort();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeShort(ptr, value);
    }

    private static void I32_STORE(MStack stack, Instance instance, long[] operands) {
        var value = stack.pop().asInt();
        var ptr = (int) (operands[1] + stack.pop().asInt());
        instance.memory().writeI32(ptr, value);
    }

    private static void I64_LOAD32_U(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readU32(ptr);
        stack.push(val);
    }

    private static void I64_LOAD32_S(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI32(ptr);
        // TODO this is a bit hacky
        stack.push(Value.i64(val.asInt()));
    }

    private static void I64_LOAD16_U(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readU16(ptr);
        // TODO this is a bit hacky
        stack.push(Value.i64(val.asInt()));
    }

    private static void I32_LOAD16_U(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readU16(ptr);
        stack.push(val);
    }

    private static void I64_LOAD16_S(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI16(ptr);
        // TODO this is a bit hacky
        stack.push(Value.i64(val.asInt()));
    }

    private static void I32_LOAD16_S(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI16(ptr);
        stack.push(val);
    }

    private static void I64_LOAD8_U(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readU8(ptr);
        // TODO a bit hacky
        stack.push(Value.i64(val.asInt()));
    }

    private static void I32_LOAD8_U(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readU8(ptr);
        stack.push(val);
    }

    private static void I64_LOAD8_S(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI8(ptr);
        // TODO a bit hacky
        stack.push(Value.i64(val.asInt()));
    }

    private static void I32_LOAD8_S(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI8(ptr);
        stack.push(val);
    }

    private static void F64_LOAD(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readF64(ptr);
        stack.push(val);
    }

    private static void F32_LOAD(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readF32(ptr);
        stack.push(val);
    }

    private static void I64_LOAD(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI64(ptr);
        stack.push(val);
    }

    private static void I32_LOAD(MStack stack, Instance instance, long[] operands) {
        var ptr = (int) (operands[1] + stack.pop().asInt());
        var val = instance.memory().readI32(ptr);
        stack.push(val);
    }

    private static void TABLE_SET(MStack stack, Instance instance, long[] operands) {
        var idx = (int) operands[0];
        var table = instance.table(idx);

        var value = stack.pop().asExtRef();
        var i = stack.pop().asInt();
        table.setRef(i, value, instance);
    }

    private static void TABLE_GET(MStack stack, Instance instance, long[] operands) {
        var idx = (int) operands[0];
        var table = instance.table(idx);

        var i = stack.pop().asInt();
        if (i < 0 || i >= table.limits().max() || i >= table.size()) {
            throw new WASMRuntimeException("out of bounds table access");
        }
        stack.push(table.ref(i));
    }

    private static void GLOBAL_SET(MStack stack, Instance instance, long[] operands) {
        var id = (int) operands[0];
        var mutabilityType =
                (instance.globalInitializer(id) == null)
                        ? instance.imports().global(id).mutabilityType()
                        : instance.globalInitializer(id);
        if (mutabilityType == MutabilityType.Const) {
            throw new RuntimeException("Can't call GLOBAL_SET on immutable global");
        }
        var val = stack.pop();
        instance.writeGlobal(id, val);
    }

    private static void GLOBAL_GET(MStack stack, Instance instance, long[] operands) {
        int idx = (int) operands[0];
        var val = instance.readGlobal(idx);

        stack.push(val);
    }

    private static void SELECT(MStack stack) {
        var pred = stack.pop().asInt();
        var b = stack.pop();
        var a = stack.pop();
        if (pred == 0) {
            stack.push(b);
        } else {
            stack.push(a);
        }
    }

    private static void SELECT_T(MStack stack, long[] operands) {
        var pred = stack.pop().asInt();
        var b = stack.pop();
        var a = stack.pop();
        if (pred == 0) {
            stack.push(b);
        } else {
            stack.push(a);
        }
    }

    private static void CALL_INDIRECT(
            MStack stack, Instance instance, ArrayDeque<StackFrame> callStack, long[] operands) {
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
    }

    private static void BLOCK(StackFrame frame, MStack stack) {
        frame.isControlFrame = true;
        frame.registerStackSize(stack);
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

    private static void IF(StackFrame frame, MStack stack, Instruction instruction) {
        frame.isControlFrame = false;
        frame.registerStackSize(stack);
        var predValue = stack.pop();
        frame.jumpTo(predValue.asInt() == 0 ? instruction.labelFalse() : instruction.labelTrue());
    }

    private static void BR_TABLE(StackFrame frame, MStack stack, Instruction instruction) {
        var predValue = prepareControlTransfer(frame, stack, true);
        var pred = predValue.asInt();

        if (pred < 0 || pred >= instruction.labelTable().length - 1) {
            // choose default
            frame.jumpTo(instruction.labelTable()[instruction.labelTable().length - 1]);
        } else {
            frame.jumpTo(instruction.labelTable()[pred]);
        }
    }

    private static void BR_IF(StackFrame frame, MStack stack, Instruction instruction) {
        var predValue = prepareControlTransfer(frame, stack, true);
        var pred = predValue.asInt();

        if (pred == 0) {
            frame.jumpTo(instruction.labelFalse());
        } else {
            frame.jumpTo(instruction.labelTrue());
        }
    }

    private static Value prepareControlTransfer(StackFrame frame, MStack stack, boolean consume) {
        frame.doControlTransfer = true;
        return consume ? stack.pop() : null;
    }

    private static void doControlTransfer(
            Instance instance, MStack stack, StackFrame frame, Instruction scope) {
        // reset the control transfer
        frame.doControlTransfer = false;

        Value[] returns = new Value[numberOfValuesToReturn(instance, scope)];
        for (int i = 0; i < returns.length; i++) {
            if (stack.size() > 0) returns[i] = stack.pop();
        }

        // drop everything till the previous label
        frame.dropValuesOutOfBlock(stack);

        for (int i = 0; i < returns.length; i++) {
            Value value = returns[returns.length - 1 - i];
            if (value != null) {
                stack.push(value);
            }
        }
    }

    public static Value computeConstantValue(Instance instance, Instruction[] expr) {
        return computeConstantValue(instance, Arrays.asList(expr));
    }

    public static Value computeConstantValue(Instance instance, List<Instruction> expr) {
        Value tos = null;
        for (Instruction instruction : expr) {
            switch (instruction.opcode()) {
                case F32_CONST:
                    {
                        tos = Value.f32(instruction.operands()[0]);
                        break;
                    }
                case F64_CONST:
                    {
                        tos = Value.f64(instruction.operands()[0]);
                        break;
                    }
                case I32_CONST:
                    {
                        tos = Value.i32(instruction.operands()[0]);
                        break;
                    }
                case I64_CONST:
                    {
                        tos = Value.i64(instruction.operands()[0]);
                        break;
                    }
                case REF_NULL:
                    {
                        ValueType vt = ValueType.refTypeForId((int) instruction.operands()[0]);
                        if (vt == ValueType.ExternRef) {
                            tos = Value.EXTREF_NULL;
                        } else if (vt == ValueType.FuncRef) {
                            tos = Value.FUNCREF_NULL;
                        } else {
                            throw new IllegalStateException(
                                    "Unexpected wrong type for ref.null instruction");
                        }
                        break;
                    }
                case REF_FUNC:
                    {
                        tos = Value.funcRef(instruction.operands()[0]);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        return instance.readGlobal((int) instruction.operands()[0]);
                    }
                case END:
                    {
                        break;
                    }
                default:
                    {
                        throw new ChicoryException(
                                "Non-constant instruction encountered: " + instruction);
                    }
            }
        }
        if (tos == null) {
            throw new ChicoryException("No constant value loaded");
        }
        return tos;
    }

    private static Value getRuntimeElementValue(Instance instance, int elemIdx, int itemIdx) {
        var elem = instance.element(elemIdx);
        return computeConstantValue(instance, elem.initializers().get(itemIdx));
    }

    List<StackFrame> getStackTrace() {
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
}
