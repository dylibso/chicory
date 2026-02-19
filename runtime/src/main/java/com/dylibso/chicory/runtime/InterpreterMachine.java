package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValType.sizeOf;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.util.Objects.requireNonNullElse;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
public class InterpreterMachine implements Machine {

    private enum AtomicOp {
        ADD,
        SUB,
        AND,
        OR,
        XOR,
        XCHG
    }

    private final MStack stack;

    protected final Deque<StackFrame> callStack;

    private final Instance instance;

    public InterpreterMachine(Instance instance) {
        this.instance = instance;
        stack = new MStack();
        this.callStack = new ArrayDeque<>();
    }

    @FunctionalInterface
    protected interface Operands {
        long get(int index);
    }

    @SuppressWarnings("DoNotCallSuggester")
    protected void evalDefault(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            Instruction instruction,
            Operands operands)
            throws ChicoryException {
        throw new RuntimeException("Machine doesn't recognize Instruction " + instruction);
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        return call(stack, instance, callStack, funcId, args, null, true);
    }

    protected long[] call(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            int funcId,
            long[] args,
            FunctionType callType,
            boolean popResults)
            throws ChicoryException {

        checkInterruption();
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);

        if (callType != null) {
            verifyIndirectCall(type, callType, instance.module().typeSection());
        }

        var func = instance.function(funcId);
        if (func != null) {
            var stackFrame =
                    new StackFrame(
                            instance,
                            funcId,
                            args,
                            type.params(),
                            func.localTypes(),
                            func.instructions());
            stackFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(stackFrame);

            try {
                eval(stack, instance, callStack);
            } catch (StackOverflowError e) {
                throw new ChicoryException("call stack exhausted", e);
            }
        } else {
            var stackFrame = new StackFrame(instance, funcId, args);
            stackFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(stackFrame);

            var imprt = instance.imports().function(funcId);

            try {
                var results = imprt.handle().apply(instance, args);
                // a host function can return null or an array of ints
                // which we will push onto the stack
                if (results != null) {
                    for (var result : results) {
                        stack.push(result);
                    }
                }
            } catch (WasmException e) {
                THROW_REF(instance, instance.registerException(e), stack, stackFrame, callStack);
            }
        }

        if (!callStack.isEmpty()) {
            callStack.pop();
        }

        if (!popResults) {
            return null;
        }

        if (type.returns().isEmpty()) {
            return null;
        }
        if (stack.size() == 0) {
            return null;
        }

        var totalResults = sizeOf(type.returns());
        var results = new long[totalResults];
        for (var i = totalResults - 1; i >= 0; i--) {
            results[i] = stack.pop();
        }
        return results;
    }

    protected Instance instance() {
        return instance;
    }

    protected MStack stack() {
        return stack;
    }

    protected void eval(MStack stack, Instance instance, Deque<StackFrame> callStack)
            throws ChicoryException {
        var frame = callStack.peek();
        boolean shouldReturn = false;

        loop:
        while (!frame.terminated()) {
            if (shouldReturn) {
                return;
            }
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
            Operands operands = instruction::operand;
            instance.onExecution(instruction, stack);
            switch (opcode) {
                case UNREACHABLE:
                    throw new TrapException("Trapped on unreachable instruction");
                case NOP:
                    break;
                case LOOP:
                case BLOCK:
                    BLOCK(frame, stack, instance, instruction);
                    break;
                case TRY_TABLE:
                    TRY_TABLE(frame, stack, instance, instruction, frame.currentPc());
                    break;
                case IF:
                    IF(frame, stack, instance, instruction);
                    break;
                case ELSE:
                    frame.jumpTo(instruction.labelTrue());
                    break;
                case BR:
                    BR(frame, stack, instruction);
                    break;
                case BR_IF:
                    BR_IF(frame, stack, instruction);
                    break;
                case BR_TABLE:
                    BR_TABLE(frame, stack, instruction);
                    break;
                case BR_ON_NULL:
                    BR_ON_NULL(frame, stack, instruction);
                    break;
                case BR_ON_NON_NULL:
                    BR_ON_NON_NULL(frame, stack, instruction);
                    break;
                case END:
                    {
                        var ctrlFrame = frame.popCtrl();
                        StackFrame.doControlTransfer(ctrlFrame, stack);

                        // if this is the last end, then we're done with
                        // the function
                        if (frame.isLastBlock()) {
                            break loop;
                        }
                        break;
                    }
                case RETURN:
                    {
                        // RETURN doesn't pass through the END
                        var ctrlFrame = frame.popCtrlTillCall();
                        StackFrame.doControlTransfer(ctrlFrame, stack);
                        callStack.clear();

                        shouldReturn = true;
                        break;
                    }
                case RETURN_CALL:
                    // swap in place the current frame
                    frame = RETURN_CALL(stack, instance, callStack, operands, frame);
                    break;
                case RETURN_CALL_INDIRECT:
                    // swap in place the current frame
                    frame = RETURN_CALL_INDIRECT(stack, instance, callStack, operands, frame);
                    break;
                case RETURN_CALL_REF:
                    // swap in place the current frame
                    frame = RETURN_CALL_REF(stack, instance, callStack, frame);
                    break;
                case THROW:
                    {
                        int tagNumber = (int) operands.get(0);
                        var tag = instance.tag(tagNumber);
                        var type = instance.type(tag.tagType().typeIdx());

                        var args = extractArgsForParams(stack, type.params());
                        var exception = new WasmException(instance, tagNumber, args);
                        var exceptionIdx = instance.registerException(exception);
                        frame = THROW_REF(instance, exceptionIdx, stack, frame, callStack);
                        break;
                    }
                case THROW_REF:
                    {
                        var exceptionIdx = (int) stack.pop();
                        frame = THROW_REF(instance, exceptionIdx, stack, frame, callStack);
                        break;
                    }
                case CALL_INDIRECT:
                    CALL_INDIRECT(stack, instance, callStack, operands);
                    break;
                case DROP:
                    DROP(stack, operands);
                    break;
                case SELECT:
                    SELECT(stack, operands);
                    break;
                case SELECT_T:
                    SELECT_T(stack, operands);
                    break;
                case LOCAL_GET:
                    LOCAL_GET(stack, operands, frame);
                    break;
                case LOCAL_SET:
                    LOCAL_SET(stack, operands, frame);
                    break;
                case LOCAL_TEE:
                    LOCAL_TEE(stack, operands, frame);
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
                    MEMORY_FILL(stack, instance);
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
                    stack.push(operands.get(0));
                    break;
                case I64_CONST:
                    stack.push(operands.get(0));
                    break;
                case F32_CONST:
                    stack.push(operands.get(0));
                    break;
                case F64_CONST:
                    stack.push(operands.get(0));
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
                    CALL(operands);
                    break;
                case CALL_REF:
                    CALL_REF();
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
                    I32_TRUNC_SAT_F64_S(stack);
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
                    MEMORY_COPY(stack, instance);
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
                    stack.push(operands.get(0));
                    break;
                case REF_NULL:
                    REF_NULL(stack);
                    break;
                case REF_IS_NULL:
                    REF_IS_NULL(stack);
                    break;
                case REF_AS_NON_NULL:
                    REF_AS_NON_NULL(stack);
                    break;
                case ELEM_DROP:
                    ELEM_DROP(instance, operands);
                    break;
                // Threads proposal:
                case I32_ATOMIC_LOAD:
                    I32_ATOMIC_LOAD(stack, instance, operands);
                    break;
                case I64_ATOMIC_LOAD:
                    I64_ATOMIC_LOAD(stack, instance, operands);
                    break;
                case I64_ATOMIC_LOAD8_U:
                    I64_ATOMIC_LOAD8_U(stack, instance, operands);
                    break;
                case I32_ATOMIC_LOAD8_U:
                    I32_ATOMIC_LOAD8_U(stack, instance, operands);
                    break;
                case I32_ATOMIC_LOAD16_U:
                    I32_ATOMIC_LOAD16_U(stack, instance, operands);
                    break;
                case I64_ATOMIC_LOAD16_U:
                    I64_ATOMIC_LOAD16_U(stack, instance, operands);
                    break;
                case I64_ATOMIC_LOAD32_U:
                    I64_ATOMIC_LOAD32_U(stack, instance, operands);
                    break;
                case I32_ATOMIC_STORE:
                    I32_ATOMIC_STORE(stack, instance, operands);
                    break;
                case I64_ATOMIC_STORE:
                    I64_ATOMIC_STORE(stack, instance, operands);
                    break;
                case I32_ATOMIC_STORE8:
                case I64_ATOMIC_STORE8:
                    I64_ATOMIC_STORE8(stack, instance, operands);
                    break;
                case I32_ATOMIC_STORE16:
                case I64_ATOMIC_STORE16:
                    I64_ATOMIC_STORE16(stack, instance, operands);
                    break;
                case I64_ATOMIC_STORE32:
                    I64_ATOMIC_STORE32(stack, instance, operands);
                    break;
                case I32_ATOMIC_RMW_ADD:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.ADD);
                    break;
                case I32_ATOMIC_RMW_SUB:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.SUB);
                    break;
                case I32_ATOMIC_RMW_AND:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.AND);
                    break;
                case I32_ATOMIC_RMW_OR:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.OR);
                    break;
                case I32_ATOMIC_RMW_XOR:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.XOR);
                    break;
                case I32_ATOMIC_RMW_XCHG:
                    I32_ATOMIC_RMW(stack, instance, operands, AtomicOp.XCHG);
                    break;
                case I32_ATOMIC_RMW_CMPXCHG:
                    I32_ATOMIC_RMW_CMPXCHG(stack, instance, operands);
                    break;
                case I64_ATOMIC_RMW_ADD:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.ADD);
                    break;
                case I64_ATOMIC_RMW_SUB:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.SUB);
                    break;
                case I64_ATOMIC_RMW_AND:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.AND);
                    break;
                case I64_ATOMIC_RMW_OR:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.OR);
                    break;
                case I64_ATOMIC_RMW_XOR:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.XOR);
                    break;
                case I64_ATOMIC_RMW_XCHG:
                    I64_ATOMIC_RMW(stack, instance, operands, AtomicOp.XCHG);
                    break;
                case I64_ATOMIC_RMW_CMPXCHG:
                    I64_ATOMIC_RMW_CMPXCHG(stack, instance, operands);
                    break;
                case I32_ATOMIC_RMW8_ADD_U:
                case I64_ATOMIC_RMW8_ADD_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.ADD);
                    break;
                case I32_ATOMIC_RMW8_SUB_U:
                case I64_ATOMIC_RMW8_SUB_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.SUB);
                    break;
                case I32_ATOMIC_RMW8_AND_U:
                case I64_ATOMIC_RMW8_AND_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.AND);
                    break;
                case I32_ATOMIC_RMW8_OR_U:
                case I64_ATOMIC_RMW8_OR_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.OR);
                    break;
                case I32_ATOMIC_RMW8_XOR_U:
                case I64_ATOMIC_RMW8_XOR_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.XOR);
                    break;
                case I32_ATOMIC_RMW8_XCHG_U:
                case I64_ATOMIC_RMW8_XCHG_U:
                    I64_ATOMIC_RMW8_U(stack, instance, operands, AtomicOp.XCHG);
                    break;
                case I32_ATOMIC_RMW8_CMPXCHG_U:
                case I64_ATOMIC_RMW8_CMPXCHG_U:
                    I64_ATOMIC_RMW8_CMPXCHG_U(stack, instance, operands);
                    break;
                case I32_ATOMIC_RMW16_ADD_U:
                case I64_ATOMIC_RMW16_ADD_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.ADD);
                    break;
                case I32_ATOMIC_RMW16_SUB_U:
                case I64_ATOMIC_RMW16_SUB_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.SUB);
                    break;
                case I32_ATOMIC_RMW16_AND_U:
                case I64_ATOMIC_RMW16_AND_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.AND);
                    break;
                case I32_ATOMIC_RMW16_OR_U:
                case I64_ATOMIC_RMW16_OR_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.OR);
                    break;
                case I32_ATOMIC_RMW16_XOR_U:
                case I64_ATOMIC_RMW16_XOR_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.XOR);
                    break;
                case I32_ATOMIC_RMW16_XCHG_U:
                case I64_ATOMIC_RMW16_XCHG_U:
                    I64_ATOMIC_RMW16_U(stack, instance, operands, AtomicOp.XCHG);
                    break;
                case I32_ATOMIC_RMW16_CMPXCHG_U:
                case I64_ATOMIC_RMW16_CMPXCHG_U:
                    I64_ATOMIC_RMW16_CMPXCHG_U(stack, instance, operands);
                    break;
                case I64_ATOMIC_RMW32_ADD_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.ADD);
                    break;
                case I64_ATOMIC_RMW32_SUB_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.SUB);
                    break;
                case I64_ATOMIC_RMW32_AND_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.AND);
                    break;
                case I64_ATOMIC_RMW32_OR_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.OR);
                    break;
                case I64_ATOMIC_RMW32_XOR_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.XOR);
                    break;
                case I64_ATOMIC_RMW32_XCHG_U:
                    I64_ATOMIC_RMW32_U(stack, instance, operands, AtomicOp.XCHG);
                    break;
                case I64_ATOMIC_RMW32_CMPXCHG_U:
                    I64_ATOMIC_RMW32_CMPXCHG_U(stack, instance, operands);
                    break;
                case MEM_ATOMIC_WAIT32:
                    MEM_ATOMIC_WAIT32(stack, instance, operands);
                    break;
                case MEM_ATOMIC_WAIT64:
                    MEM_ATOMIC_WAIT64(stack, instance, operands);
                    break;
                case MEM_ATOMIC_NOTIFY:
                    MEM_ATOMIC_NOTIFY(stack, instance, operands);
                    break;
                case ATOMIC_FENCE:
                    ATOMIC_FENCE(instance);
                    break;
                // GC opcodes
                case REF_EQ:
                    REF_EQ(stack);
                    break;
                case REF_I31:
                    REF_I31(stack);
                    break;
                case I31_GET_S:
                    I31_GET_S(stack);
                    break;
                case I31_GET_U:
                    I31_GET_U(stack);
                    break;
                case STRUCT_NEW:
                    STRUCT_NEW(stack, instance, operands);
                    break;
                case STRUCT_NEW_DEFAULT:
                    STRUCT_NEW_DEFAULT(stack, instance, operands);
                    break;
                case STRUCT_GET:
                case STRUCT_GET_S:
                case STRUCT_GET_U:
                    STRUCT_GET(stack, instance, operands, opcode);
                    break;
                case STRUCT_SET:
                    STRUCT_SET(stack, instance, operands);
                    break;
                case ARRAY_NEW:
                    ARRAY_NEW(stack, instance, operands);
                    break;
                case ARRAY_NEW_DEFAULT:
                    ARRAY_NEW_DEFAULT(stack, instance, operands);
                    break;
                case ARRAY_NEW_FIXED:
                    ARRAY_NEW_FIXED(stack, instance, operands);
                    break;
                case ARRAY_NEW_DATA:
                    ARRAY_NEW_DATA(stack, instance, operands);
                    break;
                case ARRAY_NEW_ELEM:
                    ARRAY_NEW_ELEM(stack, instance, operands);
                    break;
                case ARRAY_GET:
                case ARRAY_GET_S:
                case ARRAY_GET_U:
                    ARRAY_GET(stack, instance, operands, opcode);
                    break;
                case ARRAY_SET:
                    ARRAY_SET(stack, instance, operands);
                    break;
                case ARRAY_LEN:
                    ARRAY_LEN(stack, instance);
                    break;
                case ARRAY_FILL:
                    ARRAY_FILL(stack, instance, operands);
                    break;
                case ARRAY_COPY:
                    ARRAY_COPY(stack, instance);
                    break;
                case ARRAY_INIT_DATA:
                    ARRAY_INIT_DATA(stack, instance, operands);
                    break;
                case ARRAY_INIT_ELEM:
                    ARRAY_INIT_ELEM(stack, instance, operands);
                    break;
                case REF_TEST:
                case REF_TEST_NULL:
                    REF_TEST(stack, instance, operands, opcode);
                    break;
                case CAST_TEST:
                case CAST_TEST_NULL:
                    CAST_TEST(stack, instance, operands, opcode);
                    break;
                case BR_ON_CAST:
                    BR_ON_CAST(stack, instance, frame, instruction, operands);
                    break;
                case BR_ON_CAST_FAIL:
                    BR_ON_CAST_FAIL(stack, instance, frame, instruction, operands);
                    break;
                case ANY_CONVERT_EXTERN:
                case EXTERN_CONVERT_ANY:
                    // Identity operation at runtime: the value representation is the same
                    // for externref and anyref. No wrapping needed.
                    break;
                default:
                    {
                        evalDefault(stack, instance, callStack, instruction, operands);
                        break;
                    }
            }
        }
    }

    private static void I32_GE_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_GE_U(a, b));
    }

    private static void I64_GT_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_GT_U(a, b));
    }

    private static void I32_GE_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_GE_S(a, b));
    }

    private static void I64_GE_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_GE_U(a, b));
    }

    private static void I64_GE_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_GE_S(a, b));
    }

    private static void I32_LE_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_LE_S(a, b));
    }

    private static void I32_LE_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_LE_U(a, b));
    }

    private static void I64_LE_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_LE_S(a, b));
    }

    private static void I64_LE_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_LE_U(a, b));
    }

    private static void F32_EQ(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_EQ(a, b));
    }

    private static void F64_EQ(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_EQ(a, b));
    }

    private static void I32_CLZ(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(OpcodeImpl.I32_CLZ(tos));
    }

    private static void I32_CTZ(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(OpcodeImpl.I32_CTZ(tos));
    }

    private static void I32_POPCNT(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(OpcodeImpl.I32_POPCNT(tos));
    }

    private static void I32_ADD(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(a + b);
    }

    private static void I64_ADD(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a + b);
    }

    private static void I32_SUB(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(b - a);
    }

    private static void I64_SUB(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(b - a);
    }

    private static void I32_MUL(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a * b);
    }

    private static void I64_MUL(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a * b);
    }

    private static void I32_DIV_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_DIV_S(a, b));
    }

    private static void I32_DIV_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_DIV_U(a, b));
    }

    private static void I64_EXTEND_8_S(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_EXTEND_8_S(tos));
    }

    private static void I64_EXTEND_16_S(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_EXTEND_16_S(tos));
    }

    private static void I64_EXTEND_32_S(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_EXTEND_32_S(tos));
    }

    private static void F64_CONVERT_I64_U(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.doubleToLong(OpcodeImpl.F64_CONVERT_I64_U(tos)));
    }

    private static void F64_CONVERT_I32_U(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(Value.doubleToLong(OpcodeImpl.F64_CONVERT_I32_U(tos)));
    }

    private static void F64_CONVERT_I32_S(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(Value.doubleToLong(OpcodeImpl.F64_CONVERT_I32_S(tos)));
    }

    private static void I32_EXTEND_8_S(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(OpcodeImpl.I32_EXTEND_8_S(tos));
    }

    private static void F64_NEAREST(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_NEAREST(val)));
    }

    private static void F32_NEAREST(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_NEAREST(val)));
    }

    private static void F64_TRUNC(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_TRUNC(val)));
    }

    private static void F64_CEIL(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_CEIL(val)));
    }

    private static void F32_CEIL(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_CEIL(val)));
    }

    private static void F64_FLOOR(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_FLOOR(val)));
    }

    private static void F32_FLOOR(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_FLOOR(val)));
    }

    private static void F64_SQRT(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_SQRT(val)));
    }

    private static void F32_SQRT(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_SQRT(val)));
    }

    private static void F64_MAX(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_MAX(a, b)));
    }

    private static void F32_MAX(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_MAX(a, b)));
    }

    private static void F64_MIN(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_MIN(a, b)));
    }

    private static void F32_MIN(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_MIN(a, b)));
    }

    private static void F64_DIV(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(b / a));
    }

    private static void F32_DIV(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(b / a));
    }

    private static void F64_MUL(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(b * a));
    }

    private static void F32_MUL(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(b * a));
    }

    private static void F64_SUB(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(b - a));
    }

    private static void F32_SUB(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(b - a));
    }

    private static void F64_ADD(MStack stack) {
        var a = Value.longToDouble(stack.pop());
        var b = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(a + b));
    }

    private static void F32_ADD(MStack stack) {
        var a = Value.longToFloat(stack.pop());
        var b = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(a + b));
    }

    private static void I32_ROTR(MStack stack) {
        var c = (int) stack.pop();
        var v = (int) stack.pop();
        stack.push(OpcodeImpl.I32_ROTR(v, c));
    }

    private static void I32_ROTL(MStack stack) {
        var c = (int) stack.pop();
        var v = (int) stack.pop();
        stack.push(OpcodeImpl.I32_ROTL(v, c));
    }

    private static void I32_SHR_U(MStack stack) {
        var c = (int) stack.pop();
        var v = (int) stack.pop();
        stack.push(v >>> c);
    }

    private static void I32_SHR_S(MStack stack) {
        var c = (int) stack.pop();
        var v = (int) stack.pop();
        stack.push(v >> c);
    }

    private static void I32_SHL(MStack stack) {
        var c = (int) stack.pop();
        var v = (int) stack.pop();
        stack.push(v << c);
    }

    private static void I32_XOR(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(a ^ b);
    }

    private static void I32_OR(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(a | b);
    }

    private static void I32_AND(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(a & b);
    }

    private static void I64_POPCNT(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_POPCNT(tos));
    }

    private static void I64_CTZ(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_CTZ(tos));
    }

    private static void I64_CLZ(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_CLZ(tos));
    }

    private static void I64_ROTR(MStack stack) {
        var c = stack.pop();
        var v = stack.pop();
        stack.push(OpcodeImpl.I64_ROTR(v, c));
    }

    private static void I64_ROTL(MStack stack) {
        var c = stack.pop();
        var v = stack.pop();
        stack.push(OpcodeImpl.I64_ROTL(v, c));
    }

    private static void I64_REM_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_REM_U(a, b));
    }

    private static void I64_REM_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_REM_S(a, b));
    }

    private static void I64_SHR_U(MStack stack) {
        var c = stack.pop();
        var v = stack.pop();
        stack.push(v >>> c);
    }

    private static void I64_SHR_S(MStack stack) {
        var c = stack.pop();
        var v = stack.pop();
        stack.push(v >> c);
    }

    private static void I64_SHL(MStack stack) {
        var c = stack.pop();
        var v = stack.pop();
        stack.push(v << c);
    }

    private static void I64_XOR(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a ^ b);
    }

    private static void I64_OR(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a | b);
    }

    private static void I64_AND(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(a & b);
    }

    private static void I32_REM_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_REM_U(a, b));
    }

    private static void I32_REM_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_REM_S(a, b));
    }

    private static void I64_DIV_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_DIV_U(a, b));
    }

    private static void I64_DIV_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_DIV_S(a, b));
    }

    private static void I64_GT_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_GT_S(a, b));
    }

    private static void I32_GT_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_GT_U(a, b));
    }

    private static void I32_GT_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_GT_S(a, b));
    }

    private static void I64_LT_U(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_LT_U(a, b));
    }

    private static void I64_LT_S(MStack stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_LT_S(a, b));
    }

    private static void I32_LT_U(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_LT_U(a, b));
    }

    private static void I32_LT_S(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_LT_S(a, b));
    }

    private static void I64_EQZ(MStack stack) {
        var a = stack.pop();
        stack.push(OpcodeImpl.I64_EQZ(a));
    }

    private static void I32_EQZ(MStack stack) {
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.I32_EQZ(a));
    }

    private static void I64_NE(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(OpcodeImpl.I64_NE(a, b));
    }

    private static void I32_NE(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(OpcodeImpl.I32_NE(a, b));
    }

    private static void I64_EQ(MStack stack) {
        var a = stack.pop();
        var b = stack.pop();
        stack.push(OpcodeImpl.I64_EQ(a, b));
    }

    private static void I32_EQ(MStack stack) {
        var a = (int) stack.pop();
        var b = (int) stack.pop();
        stack.push(OpcodeImpl.I32_EQ(a, b));
    }

    private static void MEMORY_SIZE(MStack stack, Instance instance) {
        var sz = instance.memory().pages();
        stack.push(sz);
    }

    private static void I64_STORE32(MStack stack, Instance instance, Operands operands) {
        var value = stack.pop();
        var ptr = (int) (operands.get(1) + (int) stack.pop());
        instance.memory().writeI32(ptr, (int) value);
    }

    private static void I64_STORE8(MStack stack, Instance instance, Operands operands) {
        var value = (byte) stack.pop();
        var ptr = (int) (operands.get(1) + (int) stack.pop());
        instance.memory().writeByte(ptr, value);
    }

    private static void F64_PROMOTE_F32(MStack stack) {
        var tos = stack.pop();
        stack.push(Double.doubleToRawLongBits(Float.intBitsToFloat((int) tos)));
    }

    private static void F64_REINTERPRET_I64(MStack stack) {
        long tos = stack.pop();
        stack.push(Value.doubleToLong(OpcodeImpl.F64_REINTERPRET_I64(tos)));
    }

    private static void I32_WRAP_I64(MStack stack) {
        int tos = (int) stack.pop();
        stack.push(tos);
    }

    private static void I64_EXTEND_I32_S(MStack stack) {
        int tos = (int) stack.pop();
        stack.push(tos);
    }

    private static void I64_EXTEND_I32_U(MStack stack) {
        var tos = stack.pop();
        stack.push(OpcodeImpl.I64_EXTEND_I32_U((int) tos));
    }

    private static void I32_REINTERPRET_F32(MStack stack) {
        float tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I32_REINTERPRET_F32(tos));
    }

    private static void I64_REINTERPRET_F64(MStack stack) {
        double tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I64_REINTERPRET_F64(tos));
    }

    private static void F32_REINTERPRET_I32(MStack stack) {
        int tos = (int) stack.pop();
        stack.push(Value.floatToLong(OpcodeImpl.F32_REINTERPRET_I32(tos)));
    }

    private static void F32_DEMOTE_F64(MStack stack) {
        var val = Value.longToDouble(stack.pop());

        stack.push(Value.floatToLong((float) val));
    }

    private static void F32_CONVERT_I32_S(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(Value.floatToLong(OpcodeImpl.F32_CONVERT_I32_S(tos)));
    }

    private static void I32_EXTEND_16_S(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(OpcodeImpl.I32_EXTEND_16_S(tos));
    }

    private static void I64_TRUNC_F64_S(MStack stack) {
        double tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_F64_S(tos));
    }

    private static void F32_COPYSIGN(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_COPYSIGN(a, b)));
    }

    private static void F32_ABS(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_ABS(val)));
    }

    private static void F64_ABS(MStack stack) {
        var val = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_ABS(val)));
    }

    private static void F32_NE(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_NE(a, b));
    }

    private static void F64_NE(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_NE(a, b));
    }

    private static void F32_LT(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_LT(a, b));
    }

    private static void F64_LT(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_LT(a, b));
    }

    private static void F32_LE(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_LE(a, b));
    }

    private static void F64_LE(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_LE(a, b));
    }

    private static void F32_GE(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_GE(a, b));
    }

    private static void F64_GE(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_GE(a, b));
    }

    private static void F32_GT(MStack stack) {
        var b = Value.longToFloat(stack.pop());
        var a = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.F32_GT(a, b));
    }

    private static void F64_GT(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.F64_GT(a, b));
    }

    private static void F32_CONVERT_I32_U(MStack stack) {
        var tos = (int) stack.pop();
        stack.push(Value.floatToLong(OpcodeImpl.F32_CONVERT_I32_U(tos)));
    }

    private static void F32_CONVERT_I64_S(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.floatToLong(OpcodeImpl.F32_CONVERT_I64_S(tos)));
    }

    private static void REF_NULL(MStack stack) {
        stack.push(REF_NULL_VALUE);
    }

    private static void ELEM_DROP(Instance instance, Operands operands) {
        var x = (int) operands.get(0);
        instance.setElement(x, null);
    }

    private static void REF_IS_NULL(MStack stack) {
        var val = stack.pop();
        stack.push(((val == REF_NULL_VALUE) ? Value.TRUE : Value.FALSE));
    }

    private static void REF_AS_NON_NULL(MStack stack) {
        var ref = (int) stack.pop();
        stack.push(OpcodeImpl.REF_AS_NON_NULL(ref));
    }

    private static void DATA_DROP(Instance instance, Operands operands) {
        var segment = (int) operands.get(0);
        if (instance.memory() != null) {
            instance.memory().drop(segment);
        } else {
            instance.dropDataSegment(segment);
        }
    }

    private static void F64_CONVERT_I64_S(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.doubleToLong(OpcodeImpl.F64_CONVERT_I64_S(tos)));
    }

    private static void TABLE_GROW(MStack stack, Instance instance, Operands operands) {
        var tableidx = (int) operands.get(0);
        var table = instance.table(tableidx);

        var size = (int) stack.pop();
        var val = OpcodeImpl.boxForTable(stack.pop(), instance);

        var res = table.grow(size, val, instance);
        stack.push(res);
    }

    private static void TABLE_SIZE(MStack stack, Instance instance, Operands operands) {
        var tableidx = (int) operands.get(0);
        var table = instance.table(tableidx);

        stack.push(table.size());
    }

    private static void TABLE_FILL(MStack stack, Instance instance, Operands operands) {
        var tableidx = (int) operands.get(0);

        var size = (int) stack.pop();
        var val = OpcodeImpl.boxForTable(stack.pop(), instance);
        var offset = (int) stack.pop();

        OpcodeImpl.TABLE_FILL(instance, tableidx, size, val, offset);
    }

    private static void TABLE_COPY(MStack stack, Instance instance, Operands operands) {
        var tableidxSrc = (int) operands.get(1);
        var tableidxDst = (int) operands.get(0);

        var size = (int) stack.pop();
        var s = (int) stack.pop();
        var d = (int) stack.pop();

        OpcodeImpl.TABLE_COPY(instance, tableidxSrc, tableidxDst, size, s, d);
    }

    private static void MEMORY_COPY(MStack stack, Instance instance) {
        var size = (int) stack.pop();
        var offset = (int) stack.pop();
        var destination = (int) stack.pop();
        instance.memory().copy(destination, offset, size);
    }

    private static void TABLE_INIT(MStack stack, Instance instance, Operands operands) {
        var tableidx = (int) operands.get(1);
        var elementidx = (int) operands.get(0);

        var size = (int) stack.pop();
        var elemidx = (int) stack.pop();
        var offset = (int) stack.pop();

        OpcodeImpl.TABLE_INIT(instance, tableidx, elementidx, size, elemidx, offset);
    }

    private static void MEMORY_INIT(MStack stack, Instance instance, Operands operands) {
        var segmentId = (int) operands.get(0);
        var size = (int) stack.pop();
        var offset = (int) stack.pop();
        var destination = (int) stack.pop();
        instance.memory().initPassiveSegment(segmentId, destination, offset, size);
    }

    private static void I64_TRUNC_F32_S(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_F32_S(tos));
    }

    private static void I32_TRUNC_F64_U(MStack stack) {
        double tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_F64_U(tos));
    }

    private static void I32_TRUNC_F64_S(MStack stack) {
        var tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_F64_S(tos));
    }

    private static void I64_TRUNC_SAT_F64_U(MStack stack) {
        double tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_SAT_F64_U(tos));
    }

    private static void I64_TRUNC_SAT_F64_S(MStack stack) {
        var tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_SAT_F64_S(tos));
    }

    private static void I64_TRUNC_SAT_F32_U(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_SAT_F32_U(tos));
    }

    private static void I64_TRUNC_SAT_F32_S(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_SAT_F32_S(tos));
    }

    private static void I64_TRUNC_F64_U(MStack stack) {
        var tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_F64_U(tos));
    }

    private static void I64_TRUNC_F32_U(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I64_TRUNC_F32_U(tos));
    }

    private static void F32_CONVERT_I64_U(MStack stack) {
        var tos = stack.pop();
        stack.push(Value.floatToLong(OpcodeImpl.F32_CONVERT_I64_U(tos)));
    }

    private static void I32_TRUNC_F32_U(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_F32_U(tos));
    }

    private static void I32_TRUNC_SAT_F64_U(MStack stack) {
        double tos = Double.longBitsToDouble(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_SAT_F64_U(tos));
    }

    private static void I32_TRUNC_SAT_F64_S(MStack stack) {
        var tos = Value.longToDouble(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_SAT_F64_S(tos));
    }

    private static void I32_TRUNC_SAT_F32_U(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_SAT_F32_U(tos));
    }

    private static void I32_TRUNC_SAT_F32_S(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_SAT_F32_S(tos));
    }

    private static void I32_TRUNC_F32_S(MStack stack) {
        float tos = Value.longToFloat(stack.pop());
        stack.push(OpcodeImpl.I32_TRUNC_F32_S(tos));
    }

    private static void F64_COPYSIGN(MStack stack) {
        var b = Value.longToDouble(stack.pop());
        var a = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(OpcodeImpl.F64_COPYSIGN(a, b)));
    }

    private static void F32_TRUNC(MStack stack) {
        var val = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(OpcodeImpl.F32_TRUNC(val)));
    }

    protected void CALL(Operands operands) {
        var funcId = (int) operands.get(0);
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);
        // given a list of param types, let's pop those params off the stack
        // and pass as args to the function call
        var args = extractArgsForParams(stack, type.params());
        call(stack, instance, callStack, funcId, args, type, false);
    }

    private void CALL_REF() {
        int funcId = (int) stack.pop();
        if (funcId == REF_NULL_VALUE) {
            throw new TrapException("Trapped on call_ref on null function reference");
        }
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);
        // given a list of param types, let's pop those params off the stack
        // and pass as args to the function call
        var args = extractArgsForParams(stack, type.params());
        call(stack, instance, callStack, funcId, args, type, false);
    }

    private static void F64_NEG(MStack stack) {
        var tos = Value.longToDouble(stack.pop());
        stack.push(Value.doubleToLong(-tos));
    }

    private static void F32_NEG(MStack stack) {
        var tos = Value.longToFloat(stack.pop());
        stack.push(Value.floatToLong(-tos));
    }

    private static void MEMORY_FILL(MStack stack, Instance instance) {
        var size = (int) stack.pop();
        var val = (byte) stack.pop();
        var offset = (int) stack.pop();
        var end = (size + offset);
        instance.memory().fill(val, offset, end);
    }

    private static void MEMORY_GROW(MStack stack, Instance instance) {
        var size = (int) stack.pop();
        var nPages = instance.memory().grow(size);
        stack.push(nPages);
    }

    protected static int readMemPtr(MStack stack, Operands operands) {
        int address = (int) stack.pop();
        if (operands.get(1) < 0 || operands.get(1) >= Integer.MAX_VALUE || address < 0) {
            throw new WasmRuntimeException("out of bounds memory access");
        }

        return (int) (operands.get(1) + address);
    }

    private static void F64_STORE(MStack stack, Instance instance, Operands operands) {
        var value = Value.longToDouble(stack.pop());
        var ptr = readMemPtr(stack, operands);
        instance.memory().writeF64(ptr, value);
    }

    private static void F32_STORE(MStack stack, Instance instance, Operands operands) {
        var value = Value.longToFloat(stack.pop());
        var ptr = readMemPtr(stack, operands);
        instance.memory().writeF32(ptr, value);
    }

    private static void I64_STORE(MStack stack, Instance instance, Operands operands) {
        var value = stack.pop();
        var ptr = readMemPtr(stack, operands);
        instance.memory().writeLong(ptr, value);
    }

    private static void I64_STORE16(MStack stack, Instance instance, Operands operands) {
        var value = (short) stack.pop();
        var ptr = readMemPtr(stack, operands);
        instance.memory().writeShort(ptr, value);
    }

    private static void I32_STORE(MStack stack, Instance instance, Operands operands) {
        var value = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        instance.memory().writeI32(ptr, value);
    }

    private static void I64_LOAD32_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        // TODO: make all the memory.readThings to return long
        var val = instance.memory().readU32(ptr);
        stack.push(val);
    }

    private static void I64_LOAD32_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI32(ptr);
        stack.push(val);
    }

    private static void I64_LOAD16_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readU16(ptr);
        stack.push(val);
    }

    private static void I32_LOAD16_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readU16(ptr);
        stack.push(val);
    }

    private static void I64_LOAD16_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI16(ptr);
        stack.push(val);
    }

    private static void I32_LOAD16_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI16(ptr);
        stack.push(val);
    }

    private static void I64_LOAD8_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readU8(ptr);
        stack.push(val);
    }

    private static void I32_LOAD8_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readU8(ptr);
        stack.push(val);
    }

    private static void I64_LOAD8_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI8(ptr);
        stack.push(val);
    }

    private static void I32_LOAD8_S(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI8(ptr);
        stack.push(val);
    }

    private static void F64_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readF64(ptr);
        stack.push(val);
    }

    private static void F32_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readF32(ptr);
        stack.push(val);
    }

    private static void I64_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI64(ptr);
        stack.push(val);
    }

    private static void I32_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().readI32(ptr);
        stack.push(val);
    }

    private static void TABLE_SET(MStack stack, Instance instance, Operands operands) {
        var idx = (int) operands.get(0);
        var table = instance.table(idx);

        var value = OpcodeImpl.boxForTable(stack.pop(), instance);
        var i = (int) stack.pop();
        table.setRef(i, value, instance);
    }

    private static void TABLE_GET(MStack stack, Instance instance, Operands operands) {
        var idx = (int) operands.get(0);
        var table = instance.table(idx);
        var i = (int) stack.pop();
        var ref = OpcodeImpl.TABLE_GET(instance, idx, i);
        stack.push(OpcodeImpl.unboxFromTable(ref, instance, table.elementType()));
    }

    private static void GLOBAL_SET(MStack stack, Instance instance, Operands operands) {
        var id = (int) operands.get(0);
        if (!instance.global(id).getType().equals(ValType.V128)) {
            var val = stack.pop();
            instance.global(id).setValue(val);
        } else {
            var high = stack.pop();
            var low = stack.pop();
            instance.global(id).setValueLow(low);
            instance.global(id).setValueHigh(high);
        }
    }

    private static void GLOBAL_GET(MStack stack, Instance instance, Operands operands) {
        int idx = (int) operands.get(0);

        stack.push(instance.global(idx).getValueLow());
        if (instance.global(idx).getType().equals(ValType.V128)) {
            stack.push(instance.global(idx).getValueHigh());
        }
    }

    private static void DROP(MStack stack, Operands operands) {
        if (operands.get(0) == ValType.ID.V128) {
            stack.pop();
        }
        stack.pop();
    }

    private static void SELECT(MStack stack, Operands operands) {
        var pred = (int) stack.pop();
        if (operands.get(0) == ValType.ID.V128) {
            var b1 = stack.pop();
            var b2 = stack.pop();
            var a1 = stack.pop();
            var a2 = stack.pop();
            if (pred == 0) {
                stack.push(b2);
                stack.push(b1);
            } else {
                stack.push(a2);
                stack.push(a1);
            }
        } else {
            var b = stack.pop();
            var a = stack.pop();
            if (pred == 0) {
                stack.push(b);
            } else {
                stack.push(a);
            }
        }
    }

    private static void SELECT_T(MStack stack, Operands operands) {
        var pred = (int) stack.pop();
        var typeId = operands.get(0);

        if (typeId == ValType.V128.id()) {
            var b1 = stack.pop();
            var b2 = stack.pop();
            var a1 = stack.pop();
            var a2 = stack.pop();
            if (pred == 0) {
                stack.push(b2);
                stack.push(b1);
            } else {
                stack.push(a2);
                stack.push(a1);
            }
        } else {
            var b = stack.pop();
            var a = stack.pop();
            if (pred == 0) {
                stack.push(b);
            } else {
                stack.push(a);
            }
        }
    }

    private static void LOCAL_GET(MStack stack, Operands operands, StackFrame currentStackFrame) {
        var idx = (int) operands.get(0);
        var i = currentStackFrame.localIndexOf(idx);
        if (currentStackFrame.localType(idx).equals(ValType.V128)) {
            stack.push(currentStackFrame.local(i));
            stack.push(currentStackFrame.local(i + 1));
        } else {
            stack.push(currentStackFrame.local(i));
        }
    }

    private static void LOCAL_SET(MStack stack, Operands operands, StackFrame currentStackFrame) {
        var idx = (int) operands.get(0);
        var i = currentStackFrame.localIndexOf(idx);
        if (currentStackFrame.localType(idx).equals(ValType.V128)) {
            currentStackFrame.setLocal(i, stack.pop());
            currentStackFrame.setLocal(i + 1, stack.pop());
        } else {
            currentStackFrame.setLocal(i, stack.pop());
        }
    }

    private static void LOCAL_TEE(MStack stack, Operands operands, StackFrame currentStackFrame) {
        // here we peek instead of pop, leaving it on the stack
        var idx = (int) operands.get(0);
        var i = currentStackFrame.localIndexOf(idx);
        if (currentStackFrame.localType(idx).equals(ValType.V128)) {
            var tmp = stack.pop();
            currentStackFrame.setLocal(i, tmp);
            currentStackFrame.setLocal(i + 1, stack.peek());
            stack.push(tmp);
        } else {
            currentStackFrame.setLocal(i, stack.peek());
        }
    }

    private static void I32_ATOMIC_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var val = instance.memory().atomicReadInt(ptr);
        stack.push(val);
    }

    private static void I64_ATOMIC_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var val = instance.memory().atomicReadLong(ptr);
        stack.push(val);
    }

    private static void I64_ATOMIC_LOAD8_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().atomicReadByte(ptr);
        stack.push(Byte.toUnsignedLong(val));
    }

    private static void I32_ATOMIC_LOAD8_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var val = instance.memory().atomicReadByte(ptr);
        stack.push(Byte.toUnsignedLong(val));
    }

    private static void I32_ATOMIC_LOAD16_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var val = instance.memory().atomicReadShort(ptr);
        stack.push(Short.toUnsignedLong(val));
    }

    private static void I64_ATOMIC_LOAD16_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var val = instance.memory().atomicReadShort(ptr);
        stack.push(Short.toUnsignedLong(val));
    }

    private static void I64_ATOMIC_LOAD32_U(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var val = instance.memory().atomicReadInt(ptr);
        stack.push(Integer.toUnsignedLong(val));
    }

    private static void I32_ATOMIC_STORE(MStack stack, Instance instance, Operands operands) {
        var value = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        instance.memory().atomicWriteInt(ptr, value);
    }

    private static void I64_ATOMIC_STORE8(MStack stack, Instance instance, Operands operands) {
        var value = (byte) stack.pop();
        var ptr = readMemPtr(stack, operands);
        instance.memory().atomicWriteByte(ptr, value);
    }

    private static void I64_ATOMIC_STORE16(MStack stack, Instance instance, Operands operands) {
        var value = (short) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        instance.memory().atomicWriteShort(ptr, value);
    }

    private static void I64_ATOMIC_STORE32(MStack stack, Instance instance, Operands operands) {
        var value = stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        instance.memory().atomicWriteInt(ptr, (int) value);
    }

    private static void I64_ATOMIC_STORE(MStack stack, Instance instance, Operands operands) {
        var value = stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        instance.memory().atomicWriteLong(ptr, value);
    }

    private static void I32_ATOMIC_RMW(
            MStack stack, Instance instance, Operands operands, AtomicOp op) {
        var operand = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        int oldVal;
        switch (op) {
            case ADD:
                oldVal = instance.memory().atomicAddInt(ptr, operand);
                break;
            case SUB:
                oldVal = instance.memory().atomicAddInt(ptr, -operand);
                break;
            case AND:
                oldVal = instance.memory().atomicAndInt(ptr, operand);
                break;
            case OR:
                oldVal = instance.memory().atomicOrInt(ptr, operand);
                break;
            case XOR:
                oldVal = instance.memory().atomicXorInt(ptr, operand);
                break;
            case XCHG:
                oldVal = instance.memory().atomicXchgInt(ptr, operand);
                break;
            default:
                throw new IllegalStateException("Unexpected atomic op: " + op);
        }
        stack.push(oldVal);
    }

    private static void I32_ATOMIC_RMW_CMPXCHG(MStack stack, Instance instance, Operands operands) {
        var replacement = (int) stack.pop(); // c3
        var expected = (int) stack.pop(); // c2
        var ptr = readMemPtr(stack, operands); // i
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var oldVal = instance.memory().atomicCmpxchgInt(ptr, expected, replacement);
        stack.push(oldVal);
    }

    private static void I64_ATOMIC_RMW(
            MStack stack, Instance instance, Operands operands, AtomicOp op) {
        var operand = stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        long oldVal;
        switch (op) {
            case ADD:
                oldVal = instance.memory().atomicAddLong(ptr, operand);
                break;
            case SUB:
                oldVal = instance.memory().atomicAddLong(ptr, -operand);
                break;
            case AND:
                oldVal = instance.memory().atomicAndLong(ptr, operand);
                break;
            case OR:
                oldVal = instance.memory().atomicOrLong(ptr, operand);
                break;
            case XOR:
                oldVal = instance.memory().atomicXorLong(ptr, operand);
                break;
            case XCHG:
                oldVal = instance.memory().atomicXchgLong(ptr, operand);
                break;
            default:
                throw new IllegalStateException("Unexpected atomic op: " + op);
        }
        stack.push(oldVal);
    }

    private static void I64_ATOMIC_RMW_CMPXCHG(MStack stack, Instance instance, Operands operands) {
        var replacement = stack.pop();
        var expected = stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var oldVal = instance.memory().atomicCmpxchgLong(ptr, expected, replacement);
        stack.push(oldVal);
    }

    private static void I64_ATOMIC_RMW8_U(
            MStack stack, Instance instance, Operands operands, AtomicOp op) {
        var operand = (byte) stack.pop();
        var ptr = readMemPtr(stack, operands);
        byte oldVal;
        switch (op) {
            case ADD:
                oldVal = instance.memory().atomicAddByte(ptr, operand);
                break;
            case SUB:
                oldVal = instance.memory().atomicAddByte(ptr, (byte) -operand);
                break;
            case AND:
                oldVal = instance.memory().atomicAndByte(ptr, operand);
                break;
            case OR:
                oldVal = instance.memory().atomicOrByte(ptr, operand);
                break;
            case XOR:
                oldVal = instance.memory().atomicXorByte(ptr, operand);
                break;
            case XCHG:
                oldVal = instance.memory().atomicXchgByte(ptr, operand);
                break;
            default:
                throw new IllegalStateException("Unexpected atomic op: " + op);
        }
        stack.push(Byte.toUnsignedLong(oldVal));
    }

    private static void I64_ATOMIC_RMW8_CMPXCHG_U(
            MStack stack, Instance instance, Operands operands) {
        var replacement = (byte) stack.pop();
        var expected = (byte) stack.pop();
        var ptr = readMemPtr(stack, operands);
        var oldVal = instance.memory().atomicCmpxchgByte(ptr, expected, replacement);
        stack.push(Byte.toUnsignedLong(oldVal));
    }

    private static void I64_ATOMIC_RMW16_U(
            MStack stack, Instance instance, Operands operands, AtomicOp op) {
        var operand = (short) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        short oldVal;
        switch (op) {
            case ADD:
                oldVal = instance.memory().atomicAddShort(ptr, operand);
                break;
            case SUB:
                oldVal = instance.memory().atomicAddShort(ptr, (short) -operand);
                break;
            case AND:
                oldVal = instance.memory().atomicAndShort(ptr, operand);
                break;
            case OR:
                oldVal = instance.memory().atomicOrShort(ptr, operand);
                break;
            case XOR:
                oldVal = instance.memory().atomicXorShort(ptr, operand);
                break;
            case XCHG:
                oldVal = instance.memory().atomicXchgShort(ptr, operand);
                break;
            default:
                throw new IllegalStateException("Unexpected atomic op: " + op);
        }
        stack.push(Short.toUnsignedLong(oldVal));
    }

    private static void I64_ATOMIC_RMW16_CMPXCHG_U(
            MStack stack, Instance instance, Operands operands) {
        var replacement = (short) stack.pop();
        var expected = (short) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 2 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var oldVal = instance.memory().atomicCmpxchgShort(ptr, expected, replacement);
        stack.push(Short.toUnsignedLong(oldVal));
    }

    private static void I64_ATOMIC_RMW32_U(
            MStack stack, Instance instance, Operands operands, AtomicOp op) {
        var operand = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        int oldVal;
        switch (op) {
            case ADD:
                oldVal = instance.memory().atomicAddInt(ptr, operand);
                break;
            case SUB:
                oldVal = instance.memory().atomicAddInt(ptr, -operand);
                break;
            case AND:
                oldVal = instance.memory().atomicAndInt(ptr, operand);
                break;
            case OR:
                oldVal = instance.memory().atomicOrInt(ptr, operand);
                break;
            case XOR:
                oldVal = instance.memory().atomicXorInt(ptr, operand);
                break;
            case XCHG:
                oldVal = instance.memory().atomicXchgInt(ptr, operand);
                break;
            default:
                throw new IllegalStateException("Unexpected atomic op: " + op);
        }
        stack.push(Integer.toUnsignedLong(oldVal));
    }

    private static void I64_ATOMIC_RMW32_CMPXCHG_U(
            MStack stack, Instance instance, Operands operands) {
        var replacement = (int) stack.pop();
        var expected = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var oldVal = instance.memory().atomicCmpxchgInt(ptr, expected, replacement);
        stack.push(Integer.toUnsignedLong(oldVal));
    }

    private static void MEM_ATOMIC_WAIT32(MStack stack, Instance instance, Operands operands) {
        long timeout = stack.pop();
        int expected = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 4 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var result = instance.memory().atomicWait(ptr, expected, timeout);
        stack.push(result);
    }

    private static void MEM_ATOMIC_WAIT64(MStack stack, Instance instance, Operands operands) {
        long timeout = stack.pop();
        long expected = stack.pop();
        var ptr = readMemPtr(stack, operands);
        if (ptr % 8 != 0) {
            throw new InvalidException("unaligned atomic");
        }
        var result = instance.memory().atomicWait(ptr, expected, timeout);
        stack.push(result);
    }

    private static void MEM_ATOMIC_NOTIFY(MStack stack, Instance instance, Operands operands) {
        int maxThreads = (int) stack.pop();
        var ptr = readMemPtr(stack, operands);
        var result = instance.memory().atomicNotify(ptr, maxThreads);
        stack.push(result);
    }

    private static void ATOMIC_FENCE(Instance instance) {
        instance.memory().atomicFence();
    }

    private static StackFrame RETURN_CALL(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            Operands operands,
            StackFrame currentStackFrame) {
        var funcId = (int) operands.get(0);
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);
        var func = instance.function(funcId);
        var args = extractArgsForParams(stack, type.params());

        // optimizing when the tail call happens in the same function
        if (currentStackFrame.funcId() == funcId) {
            var ctrlFrame = currentStackFrame.popCtrlTillCall();
            StackFrame.doControlTransfer(ctrlFrame, stack);
            currentStackFrame.reset(args);
            currentStackFrame.pushCtrl(ctrlFrame);
            return currentStackFrame;
        } else {
            var fromCallStack = !callStack.isEmpty();
            var ctrlFrame =
                    (fromCallStack)
                            ? callStack.pop().popCtrlTillCall()
                            : currentStackFrame.popCtrlTillCall();
            StackFrame.doControlTransfer(ctrlFrame, stack);

            if (func != null) {
                var newFrame =
                        new StackFrame(
                                instance,
                                funcId,
                                args,
                                type.params(),
                                func.localTypes(),
                                func.instructions());
                newFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
                if (fromCallStack) {
                    callStack.push(newFrame);
                }
                return newFrame;
            } else {
                var newFrame = new StackFrame(instance, funcId, args);
                newFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
                callStack.push(newFrame);

                var imprt = instance.imports().function(funcId);

                try {
                    var results = imprt.handle().apply(instance, args);
                    // a host function can return null or an array of ints
                    // which we will push onto the stack
                    if (results != null) {
                        for (var result : results) {
                            stack.push(result);
                        }
                    }
                } catch (WasmException e) {
                    THROW_REF(instance, instance.registerException(e), stack, newFrame, callStack);
                }
                if (fromCallStack) {
                    callStack.push(newFrame);
                }
                return newFrame;
            }
        }
    }

    private static StackFrame RETURN_CALL_INDIRECT(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            Operands operands,
            StackFrame currentStackFrame) {
        var tableIdx = (int) operands.get(1);
        var table = instance.table(tableIdx);

        var typeId = (int) operands.get(0);
        int funcTableIdx = (int) stack.pop();

        int funcId = table.requiredRef(funcTableIdx);
        var refInstance = requireNonNullElse(table.instance(funcTableIdx), instance);
        var type = refInstance.type(typeId);

        // Verify type match using nominal type indices
        var actualTypeIdx = refInstance.functionType(funcId);
        verifyIndirectCallByTypeIdx(actualTypeIdx, typeId, refInstance.module().typeSection());

        var refMachine = refInstance.getMachine().getClass();
        if (!refInstance.equals(instance) && !refMachine.equals(instance.getMachine().getClass())) {
            throw new ChicoryException(
                    "Indirect tail-call to a different Machine implementation is not supported: "
                            + refMachine.getName());
        }

        var args = extractArgsForParams(stack, type.params());

        // optimizing when the tail call happens in the same function
        if (currentStackFrame.funcId() == funcId) {
            var ctrlFrame = currentStackFrame.popCtrlTillCall();
            StackFrame.doControlTransfer(ctrlFrame, stack);
            currentStackFrame.reset(args);
            currentStackFrame.pushCtrl(ctrlFrame);
            return currentStackFrame;
        } else {
            var func = instance.function(funcId);
            var fromCallStack = !callStack.isEmpty();

            if (func != null) {
                var ctrlFrame =
                        (fromCallStack)
                                ? callStack.pop().popCtrlTillCall()
                                : currentStackFrame.popCtrlTillCall();
                StackFrame.doControlTransfer(ctrlFrame, stack);
                var newFrame =
                        new StackFrame(
                                instance,
                                funcId,
                                args,
                                type.params(),
                                func.localTypes(),
                                func.instructions());
                newFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
                if (fromCallStack) {
                    callStack.push(newFrame);
                }
                return newFrame;
            } else {
                var newFrame = new StackFrame(instance, funcId, args);
                newFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
                callStack.push(newFrame);

                var imprt = instance.imports().function(funcId);

                try {
                    var results = imprt.handle().apply(instance, args);
                    // a host function can return null or an array of ints
                    // which we will push onto the stack
                    if (results != null) {
                        for (var result : results) {
                            stack.push(result);
                        }
                    }
                } catch (WasmException e) {
                    THROW_REF(instance, instance.registerException(e), stack, newFrame, callStack);
                }
                if (fromCallStack) {
                    callStack.push(newFrame);
                }
                return newFrame;
            }
        }
    }

    private static StackFrame RETURN_CALL_REF(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            StackFrame currentStackFrame) {
        int funcId = (int) stack.pop();
        if (funcId == REF_NULL_VALUE) {
            throw new TrapException("Trapped on call_ref on null function reference");
        }
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);
        var func = instance.function(funcId);
        // given a list of param types, let's pop those params off the stack
        // and pass as args to the function call
        var args = extractArgsForParams(stack, type.params());

        // optimizing when the tail call happens in the same function
        if (currentStackFrame.funcId() == funcId) {
            var ctrlFrame = currentStackFrame.popCtrlTillCall();
            StackFrame.doControlTransfer(ctrlFrame, stack);
            currentStackFrame.reset(args);
            currentStackFrame.pushCtrl(ctrlFrame);
            return currentStackFrame;
        } else {
            var ctrlFrame = callStack.pop();
            StackFrame.doControlTransfer(ctrlFrame.popCtrlTillCall(), stack);
            var newFrame =
                    new StackFrame(
                            instance,
                            funcId,
                            args,
                            type.params(),
                            func.localTypes(),
                            func.instructions());
            newFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(newFrame);
            return newFrame;
        }
    }

    private void CALL_INDIRECT(
            MStack stack, Instance instance, Deque<StackFrame> callStack, Operands operands) {
        var tableIdx = (int) operands.get(1);
        var table = instance.table(tableIdx);

        var typeId = (int) operands.get(0);
        int funcTableIdx = (int) stack.pop();

        int funcId = table.requiredRef(funcTableIdx);
        var refInstance = requireNonNullElse(table.instance(funcTableIdx), instance);
        var type = refInstance.type(typeId);

        // Verify type match using nominal type indices
        var actualTypeIdx = refInstance.functionType(funcId);
        verifyIndirectCallByTypeIdx(actualTypeIdx, typeId, refInstance.module().typeSection());

        // given a list of param types, let's pop those params off the stack
        // and pass as args to the function call
        var args = extractArgsForParams(stack, type.params());
        if (useCurrentInstanceInterpreter(instance, refInstance, funcId)) {
            call(stack, instance, callStack, funcId, args, null, false);
        } else {
            checkInterruption();
            var results = refInstance.getMachine().call(funcId, args);
            if (results != null) {
                for (var result : results) {
                    stack.push(result);
                }
            }
        }
    }

    protected boolean useCurrentInstanceInterpreter(
            Instance instance, Instance refInstance, int funcId) {
        return refInstance.equals(instance);
    }

    private static int numberOfParams(Instance instance, AnnotatedInstruction scope) {
        var typeId = (int) scope.operand(0);
        if (typeId == 0x40) { // epsilon
            return 0;
        }
        if (ValType.isValid(typeId)) {
            return 0;
        }
        return sizeOf(instance.type(typeId).params());
    }

    private static int numberOfValuesToReturn(Instance instance, AnnotatedInstruction scope) {
        if (scope.opcode() == OpCode.END) {
            return 0;
        }
        var typeId = (int) scope.operand(0);
        if (typeId == 0x40) { // epsilon
            return 0;
        }
        if (ValType.isValid(typeId)) {
            if (typeId == ValType.V128.id()) {
                return 2;
            } else {
                return 1;
            }
        }
        return sizeOf(instance.type(typeId).returns());
    }

    protected static StackFrame THROW_REF(
            Instance instance,
            int exceptionIdx,
            MStack stack,
            StackFrame frame,
            Deque<StackFrame> callStack) {
        var exception = instance.exn(exceptionIdx);
        boolean found = false;
        while (!found) {
            while (frame.ctrlStackSize() > 0) {
                var ctrlFrame = frame.popCtrl();
                if (ctrlFrame.opCode != OpCode.TRY_TABLE) {
                    continue;
                }

                frame.jumpTo(ctrlFrame.pc);
                var tryInst = frame.loadCurrentInstruction();

                var catches = tryInst.catches();
                for (int i = 0; i < catches.size() && !found; i++) {
                    var currentCatch = catches.get(i);

                    // verify import compatibility
                    var compatibleImport = false;
                    if ((currentCatch.opcode() == CatchOpCode.CATCH
                            || currentCatch.opcode() == CatchOpCode.CATCH_REF)) {
                        var currentCatchTag = instance.tag(currentCatch.tag());
                        var exceptionTag = exception.instance().tag(exception.tagIdx());

                        // if it's an import we verify the compatibility
                        if (currentCatch.tag() < instance.imports().tagCount()
                                && currentCatchTag.type().paramsMatch(exceptionTag.type())
                                && currentCatchTag.type().returnsMatch(exceptionTag.type())) {
                            compatibleImport = true;
                        } else if (exceptionTag != currentCatchTag) {
                            // if it's not an import the tag should be the same
                            continue;
                        }
                    }

                    switch (currentCatch.opcode()) {
                        case CATCH:
                            if (currentCatch.tag() == exception.tagIdx() || compatibleImport) {
                                found = true;
                                for (var arg : exception.args()) {
                                    stack.push(arg);
                                }
                            }
                            break;
                        case CATCH_REF:
                            if (currentCatch.tag() == exception.tagIdx() || compatibleImport) {
                                found = true;
                                for (var arg : exception.args()) {
                                    stack.push(arg);
                                }
                                stack.push(exceptionIdx);
                            }
                            break;
                        case CATCH_ALL:
                            found = true;
                            break;
                        case CATCH_ALL_REF:
                            found = true;
                            stack.push(exceptionIdx);
                            break;
                    }

                    if (found) {
                        // BR l
                        ctrlJump(frame, stack, currentCatch.label());
                        frame.jumpTo(currentCatch.resolvedLabel());
                        return frame;
                    }
                }
            }
            if (!found) {
                if (callStack.isEmpty()) {
                    throw exception;
                } else {
                    frame = callStack.pop();
                }
            }
        }
        throw new RuntimeException("unreacheable");
    }

    private static void BLOCK(
            StackFrame frame, MStack stack, Instance instance, AnnotatedInstruction instruction) {
        var paramsSize = numberOfParams(instance, instruction);
        var returnsSize = numberOfValuesToReturn(instance, instruction);
        frame.pushCtrl(instruction.opcode(), paramsSize, returnsSize, stack.size() - paramsSize);
    }

    private static void TRY_TABLE(
            StackFrame frame,
            MStack stack,
            Instance instance,
            AnnotatedInstruction instruction,
            int pc) {
        var paramsSize = numberOfParams(instance, instruction);
        var returnsSize = numberOfValuesToReturn(instance, instruction);
        frame.pushCtrl(
                instruction.opcode(), paramsSize, returnsSize, stack.size() - paramsSize, pc);
    }

    private static void IF(
            StackFrame frame, MStack stack, Instance instance, AnnotatedInstruction instruction) {
        var predValue = stack.pop();
        var paramsSize = numberOfParams(instance, instruction);
        var returnsSize = numberOfValuesToReturn(instance, instruction);
        frame.pushCtrl(instruction.opcode(), paramsSize, returnsSize, stack.size() - paramsSize);

        frame.jumpTo(predValue == 0 ? instruction.labelFalse() : instruction.labelTrue());
    }

    private static void ctrlJump(StackFrame frame, MStack stack, int n) {
        var ctrlFrame = frame.popCtrl(n);
        frame.pushCtrl(ctrlFrame);
        // a LOOP jumps back to the first instruction without passing through an END
        if (ctrlFrame.opCode == OpCode.LOOP) {
            StackFrame.doControlTransfer(ctrlFrame, stack);
        }
    }

    private static void BR(StackFrame frame, MStack stack, AnnotatedInstruction instruction) {
        checkInterruption();
        ctrlJump(frame, stack, (int) instruction.operand(0));
        frame.jumpTo(instruction.labelTrue());
    }

    private static void BR_TABLE(StackFrame frame, MStack stack, AnnotatedInstruction instruction) {
        var pred = (int) stack.pop();

        var defaultIdx = instruction.operandCount() - 1;
        if (pred < 0 || pred >= defaultIdx) {
            // choose default
            ctrlJump(frame, stack, (int) instruction.operand(defaultIdx));
            frame.jumpTo(instruction.labelTable().get(defaultIdx));
        } else {
            ctrlJump(frame, stack, (int) instruction.operand(pred));
            frame.jumpTo(instruction.labelTable().get(pred));
        }
    }

    private static void BR_IF(StackFrame frame, MStack stack, AnnotatedInstruction instruction) {
        var pred = (int) stack.pop();

        if (pred == 0) {
            frame.jumpTo(instruction.labelFalse());
        } else {
            ctrlJump(frame, stack, (int) instruction.operand(0));
            frame.jumpTo(instruction.labelTrue());
        }
    }

    private static void BR_ON_NULL(
            StackFrame frame, MStack stack, AnnotatedInstruction instruction) {
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            BR(frame, stack, instruction);
        } else {
            stack.push(ref);
        }
    }

    private static void BR_ON_NON_NULL(
            StackFrame frame, MStack stack, AnnotatedInstruction instruction) {
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            // do nothing
        } else {
            stack.push(ref);
            BR(frame, stack, instruction);
        }
    }

    protected static long[] extractArgsForParams(MStack stack, List<ValType> params) {
        if (params == null) {
            return Value.EMPTY_VALUES;
        }
        var args = new long[sizeOf(params)];
        for (var i = 0; i < args.length; i++) {
            args[args.length - i - 1] = stack.pop();
        }
        return args;
    }

    private static boolean functionTypeMatch(
            FunctionType actual, FunctionType expected, TypeSection ts) {
        if (actual.params().size() != expected.params().size()
                || actual.returns().size() != expected.returns().size()) {
            return false;
        }

        for (int i = 0; i < actual.params().size(); i++) {
            var actualParam = actual.params().get(i);
            var expectedParam = expected.params().get(i);

            // Contravariant: expected.param <: actual.param
            if (!ValType.matches(expectedParam, actualParam, ts)) {
                return false;
            }
        }

        for (int i = 0; i < actual.returns().size(); i++) {
            var actualReturn = actual.returns().get(i);
            var expectedReturn = expected.returns().get(i);

            // Covariant: actual.return <: expected.return
            if (!ValType.matches(actualReturn, expectedReturn, ts)) {
                return false;
            }
        }

        return true;
    }

    protected static void verifyIndirectCall(
            FunctionType actual, FunctionType expected, TypeSection ts) throws ChicoryException {
        if (!functionTypeMatch(actual, expected, ts)) {
            throw new ChicoryException("indirect call type mismatch");
        }
    }

    protected static void verifyIndirectCallByTypeIdx(
            int actualTypeIdx, int expectedTypeIdx, TypeSection ts) throws ChicoryException {
        if (actualTypeIdx != expectedTypeIdx
                && !ValType.heapTypeSubtype(actualTypeIdx, expectedTypeIdx, ts)) {
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
            throw new ChicoryInterruptedException("Thread interrupted");
        }
    }

    // ===== GC opcode implementations =====

    private static void REF_EQ(MStack stack) {
        var b = (int) stack.pop();
        var a = (int) stack.pop();
        stack.push(OpcodeImpl.REF_EQ(a, b));
    }

    private static void REF_I31(MStack stack) {
        var val = (int) stack.pop();
        stack.push(Value.encodeI31(val));
    }

    private static void I31_GET_S(MStack stack) {
        var ref = stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null i31 reference");
        }
        stack.push(Value.decodeI31S(ref));
    }

    private static void I31_GET_U(MStack stack) {
        var ref = stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null i31 reference");
        }
        stack.push(Value.decodeI31U(ref));
    }

    private static void STRUCT_NEW(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var fields = new long[st.fieldTypes().length];
        // Pop fields in reverse order (last field on top)
        for (int i = fields.length - 1; i >= 0; i--) {
            fields[i] = stack.pop();
        }
        var struct = new WasmStruct(typeIdx, fields);
        stack.push(instance.registerGcRef(struct));
    }

    private static void STRUCT_NEW_DEFAULT(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var fields = new long[st.fieldTypes().length];
        // Default values: 0 for numeric, REF_NULL_VALUE for references
        for (int i = 0; i < fields.length; i++) {
            var ft = st.fieldTypes()[i];
            if (ft.storageType().valType() != null && ft.storageType().valType().isReference()) {
                fields[i] = REF_NULL_VALUE;
            }
            // numeric types default to 0 (already zero-initialized)
        }
        var struct = new WasmStruct(typeIdx, fields);
        stack.push(instance.registerGcRef(struct));
    }

    private static void STRUCT_GET(
            MStack stack, Instance instance, Operands operands, OpCode opcode) {
        var typeIdx = (int) operands.get(0);
        var fieldIdx = (int) operands.get(1);
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        var val = struct.field(fieldIdx);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var ft = st.fieldTypes()[fieldIdx];
        if (ft.storageType().packedType() != null) {
            if (opcode == OpCode.STRUCT_GET_S) {
                val = ft.storageType().packedType().signExtend(val);
            } else {
                val = val & ft.storageType().packedType().mask();
            }
        }
        stack.push(val);
    }

    private static void STRUCT_SET(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var fieldIdx = (int) operands.get(1);
        var val = stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null structure reference");
        }
        var struct = (WasmStruct) instance.gcRef(ref);
        var st = instance.module().typeSection().getSubType(typeIdx).compType().structType();
        var ft = st.fieldTypes()[fieldIdx];
        if (ft.storageType().packedType() != null) {
            val = val & ft.storageType().packedType().mask();
        }
        struct.setField(fieldIdx, val);
    }

    private static void ARRAY_NEW(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var len = (int) stack.pop();
        var initVal = stack.pop();
        var elems = new long[len];
        java.util.Arrays.fill(elems, initVal);
        var arr = new WasmArray(typeIdx, elems);
        stack.push(instance.registerGcRef(arr));
    }

    private static void ARRAY_NEW_DEFAULT(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var len = (int) stack.pop();
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elems = new long[len];
        if (at.fieldType().storageType().valType() != null
                && at.fieldType().storageType().valType().isReference()) {
            java.util.Arrays.fill(elems, REF_NULL_VALUE);
        }
        var arr = new WasmArray(typeIdx, elems);
        stack.push(instance.registerGcRef(arr));
    }

    private static void ARRAY_NEW_FIXED(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var len = (int) operands.get(1);
        var elems = new long[len];
        for (int i = len - 1; i >= 0; i--) {
            elems[i] = stack.pop();
        }
        var arr = new WasmArray(typeIdx, elems);
        stack.push(instance.registerGcRef(arr));
    }

    private static void ARRAY_NEW_DATA(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var dataIdx = (int) operands.get(1);
        var len = (int) stack.pop();
        var offset = (int) stack.pop();
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elemSize = at.fieldType().storageType().byteSize();
        var data = instance.dataSegmentData(dataIdx);
        if ((long) offset + (long) len * elemSize > data.length) {
            throw new TrapException("out of bounds memory access");
        }
        var elems = new long[len];
        for (int i = 0; i < len; i++) {
            var byteOff = offset + i * elemSize;
            elems[i] = readFromData(data, byteOff, elemSize);
        }
        var arr = new WasmArray(typeIdx, elems);
        stack.push(instance.registerGcRef(arr));
    }

    private static void ARRAY_NEW_ELEM(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var elemIdx = (int) operands.get(1);
        var len = (int) stack.pop();
        var offset = (int) stack.pop();
        var element = instance.element(elemIdx);
        if (element == null || offset + len > element.elementCount()) {
            throw new TrapException("out of bounds table access");
        }
        var elems = new long[len];
        for (int i = 0; i < len; i++) {
            var init = element.initializers().get(offset + i);
            elems[i] = ConstantEvaluators.computeConstantValue(instance, init)[0];
        }
        var arr = new WasmArray(typeIdx, elems);
        stack.push(instance.registerGcRef(arr));
    }

    private static void ARRAY_GET(
            MStack stack, Instance instance, Operands operands, OpCode opcode) {
        var typeIdx = (int) operands.get(0);
        var idx = (int) stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var val = arr.get(idx);
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            if (opcode == OpCode.ARRAY_GET_S) {
                val = at.fieldType().storageType().packedType().signExtend(val);
            } else {
                val = val & at.fieldType().storageType().packedType().mask();
            }
        }
        stack.push(val);
    }

    private static void ARRAY_SET(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var val = stack.pop();
        var idx = (int) stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (idx < 0 || idx >= arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = val & at.fieldType().storageType().packedType().mask();
        }
        arr.set(idx, val);
    }

    private static void ARRAY_LEN(MStack stack, Instance instance) {
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        stack.push(arr.length());
    }

    private static void ARRAY_FILL(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var len = (int) stack.pop();
        var val = stack.pop();
        var offset = (int) stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        if (offset + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        if (at.fieldType().storageType().packedType() != null) {
            val = val & at.fieldType().storageType().packedType().mask();
        }
        for (int i = 0; i < len; i++) {
            arr.set(offset + i, val);
        }
    }

    private static void ARRAY_COPY(MStack stack, Instance instance) {
        // operands 0 and 1 are dst/src type indices (used for validation, not needed at runtime)
        var len = (int) stack.pop();
        var srcOffset = (int) stack.pop();
        var srcRef = (int) stack.pop();
        var dstOffset = (int) stack.pop();
        var dstRef = (int) stack.pop();
        if (dstRef == REF_NULL_VALUE || srcRef == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var dst = (WasmArray) instance.gcRef(dstRef);
        var src = (WasmArray) instance.gcRef(srcRef);
        if (dstOffset + len > dst.length() || srcOffset + len > src.length()) {
            throw new TrapException("out of bounds array access");
        }
        // Handle overlapping copies
        if (dstOffset <= srcOffset) {
            for (int i = 0; i < len; i++) {
                dst.set(dstOffset + i, src.get(srcOffset + i));
            }
        } else {
            for (int i = len - 1; i >= 0; i--) {
                dst.set(dstOffset + i, src.get(srcOffset + i));
            }
        }
    }

    private static void ARRAY_INIT_DATA(MStack stack, Instance instance, Operands operands) {
        var typeIdx = (int) operands.get(0);
        var dataIdx = (int) operands.get(1);
        var len = (int) stack.pop();
        var srcOffset = (int) stack.pop();
        var dstOffset = (int) stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        var at = instance.module().typeSection().getSubType(typeIdx).compType().arrayType();
        var elemSize = at.fieldType().storageType().byteSize();
        var data = instance.dataSegmentData(dataIdx);
        if (dstOffset + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        if ((long) srcOffset + (long) len * elemSize > data.length) {
            throw new TrapException("out of bounds memory access");
        }
        for (int i = 0; i < len; i++) {
            var byteOff = srcOffset + i * elemSize;
            arr.set(dstOffset + i, readFromData(data, byteOff, elemSize));
        }
    }

    private static void ARRAY_INIT_ELEM(MStack stack, Instance instance, Operands operands) {
        // operand 0 is the type index (used for validation, not needed at runtime)
        var elemIdx = (int) operands.get(1);
        var len = (int) stack.pop();
        var srcOffset = (int) stack.pop();
        var dstOffset = (int) stack.pop();
        var ref = (int) stack.pop();
        if (ref == REF_NULL_VALUE) {
            throw new TrapException("null array reference");
        }
        var arr = (WasmArray) instance.gcRef(ref);
        var element = instance.element(elemIdx);
        if (dstOffset + len > arr.length()) {
            throw new TrapException("out of bounds array access");
        }
        // Dropped segments have element count 0
        var elementCount = (element == null) ? 0 : element.elementCount();
        if (srcOffset + len > elementCount) {
            throw new TrapException("out of bounds table access");
        }
        if (len == 0) {
            return;
        }
        for (int i = 0; i < len; i++) {
            var init = element.initializers().get(srcOffset + i);
            arr.set(dstOffset + i, ConstantEvaluators.computeConstantValue(instance, init)[0]);
        }
    }

    private static void REF_TEST(
            MStack stack, Instance instance, Operands operands, OpCode opcode) {
        var heapType = (int) operands.get(0);
        var sourceHeapType = (int) operands.get(1);
        var ref = stack.pop();
        boolean nullable = (opcode == OpCode.REF_TEST_NULL);
        stack.push(
                instance.heapTypeMatch(ref, nullable, heapType, sourceHeapType)
                        ? Value.TRUE
                        : Value.FALSE);
    }

    private static void CAST_TEST(
            MStack stack, Instance instance, Operands operands, OpCode opcode) {
        var heapType = (int) operands.get(0);
        var sourceHeapType = (int) operands.get(1);
        var ref = stack.pop();
        boolean nullable = (opcode == OpCode.CAST_TEST_NULL);
        if (!instance.heapTypeMatch(ref, nullable, heapType, sourceHeapType)) {
            throw new TrapException("cast failure");
        }
        stack.push(ref);
    }

    private static void BR_ON_CAST(
            MStack stack,
            Instance instance,
            StackFrame frame,
            AnnotatedInstruction instruction,
            Operands operands) {
        var flags = (int) operands.get(0);
        var ht2 = (int) operands.get(3);
        var sourceHeapType = (int) operands.get(4);
        boolean null2 = (flags & 2) != 0;
        var ref = stack.pop();
        if (instance.heapTypeMatch(ref, null2, ht2, sourceHeapType)) {
            stack.push(ref);
            ctrlJump(frame, stack, (int) operands.get(1));
            frame.jumpTo(instruction.labelTrue());
        } else {
            stack.push(ref);
        }
    }

    private static void BR_ON_CAST_FAIL(
            MStack stack,
            Instance instance,
            StackFrame frame,
            AnnotatedInstruction instruction,
            Operands operands) {
        var flags = (int) operands.get(0);
        var ht2 = (int) operands.get(3);
        var sourceHeapType = (int) operands.get(4);
        boolean null2 = (flags & 2) != 0;
        var ref = stack.pop();
        if (!instance.heapTypeMatch(ref, null2, ht2, sourceHeapType)) {
            stack.push(ref);
            ctrlJump(frame, stack, (int) operands.get(1));
            frame.jumpTo(instruction.labelTrue());
        } else {
            stack.push(ref);
        }
    }

    private static long readFromData(byte[] data, int offset, int size) {
        long val = 0;
        for (int i = 0; i < size; i++) {
            val |= (long) (data[offset + i] & 0xFF) << (i * 8);
        }
        return val;
    }
}
