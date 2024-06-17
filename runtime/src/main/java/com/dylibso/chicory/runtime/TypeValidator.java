package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.List;

// Heavily inspired by wazero
// https://github.com/tetratelabs/wazero/blob/5a8a053bff0ae795b264de9672016745cb842070/internal/wasm/func_validation.go
public class TypeValidator {

    private List<ValueType> valueTypeStack = new ArrayList<>();
    private List<List<ValueType>> returns = new ArrayList<>();
    private List<List<ValueType>> prevStack = new ArrayList<>();

    private static <T> T peek(List<T> list) {
        return list.get(list.size() - 1);
    }

    private static <T> T pop(List<T> list) {
        var val = list.get(list.size() - 1);
        list.remove(list.size() - 1);
        return val;
    }

    private static <T> void push(List<T> list, T elem) {
        list.add(elem);
    }

    private static <T> List<T> clone(List<T> list) {
        return new ArrayList<>(list);
    }

    private void popAndVerifyType(ValueType expected) {
        ValueType have = null;
        if (valueTypeStack.size() > prevStack.size()) {
            have = pop(valueTypeStack);
        } else if (valueTypeStack.size() > 0) {
            // a block can consume elements outside of it
            // but they should be restored on exit
            have = pop(valueTypeStack);
        }
        verifyType(expected, have);
    }

    private void verifyType(ValueType expected, ValueType have) {
        if (have == null) {
            var expectedType = (expected == null) ? "any" : expected.name();
            throw new InvalidException(
                    "type mismatch: expected [" + expectedType + "], but was []");
        }
        if (expected != null
                && have != expected
                && have != ValueType.UNKNOWN
                && expected != ValueType.UNKNOWN) {
            throw new InvalidException(
                    "type mismatch: expected [" + expected + "], but was " + have);
        }
    }

    private static void validateMemory(Instance instance, int memIds) {
        validateMemory(instance, memIds, -1);
    }

    private static void validateMemory(Instance instance, int memIds, int dataSegmentIdx) {
        if (instance.memory() == null || memIds > 0) {
            throw new InvalidException("unknown memory " + memIds);
        }
        if (instance.memory().dataSegments() == null
                || dataSegmentIdx >= instance.memory().dataSegments().length) {
            throw new InvalidException("unknown data segment " + dataSegmentIdx);
        }
    }

    private void validateReturns(List<ValueType> returns, int limit) {
        if (returns.size() > (valueTypeStack.size() - limit)) {
            throw new InvalidException("type mismatch, not enough values to return");
        }

        var stackSize = valueTypeStack.size();
        var offset = stackSize - returns.size();
        for (int j = 0; j < returns.size(); j++) {
            var elem = valueTypeStack.get(offset + j);
            verifyType(returns.get(j), elem);
        }
    }

    private int jumpToNextEnd(List<Instruction> instructions, Instruction op, int currentPos) {
        Instruction tmpInstruction;
        var offset = 0;
        do {
            offset++;
            if (instructions.size() > (currentPos + offset)) {
                tmpInstruction = instructions.get(currentPos + offset);
            } else {
                break;
            }
        } while (tmpInstruction.depth() == (op.depth() + 1)
                && tmpInstruction.opcode() != OpCode.END);

        return offset + currentPos - 1;
    }

    private int jumpToNextElse(List<Instruction> instructions, Instruction op, int currentPos) {
        Instruction tmpInstruction = null;
        var offset = 0;
        do {
            offset++;
            if (instructions.size() > (currentPos + offset)
                    && instructions.get(currentPos + offset).opcode() == OpCode.ELSE) {
                tmpInstruction = instructions.get(currentPos + offset);
            } else {
                break;
            }
        } while (tmpInstruction.depth() == op.depth());

        if (tmpInstruction == null) {
            return -1;
        } else {
            return offset + currentPos - 1;
        }
    }

    private List<ValueType> getReturns(Instruction op, Instance instance) {
        var typeId = (int) op.scope().operands()[0];
        if (typeId == 0x40) { // epsilon
            return List.of();
        } else if (ValueType.isValid(typeId)) {
            return List.of(ValueType.forId(typeId));
        } else {
            return instance.type(typeId).returns();
        }
    }

    private void resetToDepth(int depth) {
        while (prevStack.size() > depth + 1) {
            pop(prevStack);
        }
        while (returns.size() > depth + 1) {
            pop(returns);
        }
    }

    private static ValueType getLocalType(List<ValueType> localTypes, int idx) {
        if (idx >= localTypes.size()) {
            throw new InvalidException("unknown local");
        }
        return localTypes.get(idx);
    }

    public void validate(FunctionBody body, FunctionType functionType, Instance instance) {
        var localTypes = body.localTypes();
        var inputLen = functionType.params().size();
        push(prevStack, clone(valueTypeStack));
        push(returns, functionType.returns());

        for (var i = 0; i < body.instructions().size(); i++) {
            var op = body.instructions().get(i);

            // control flow instructions handling
            switch (op.opcode()) {
                case LOOP:
                case BLOCK:
                    {
                        push(prevStack, clone(valueTypeStack));
                        push(returns, getReturns(op, instance));
                        break;
                    }
                case IF:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(prevStack, clone(valueTypeStack));
                        break;
                    }
                case ELSE:
                    {
                        valueTypeStack = pop(prevStack);
                        push(prevStack, clone(valueTypeStack));
                        break;
                    }
                case RETURN:
                    {
                        var limit = peek(prevStack).size();
                        validateReturns(functionType.returns(), limit);

                        var nextElseIdx = jumpToNextElse(body.instructions(), op, i);
                        if (nextElseIdx != -1) {
                            var nextElse = body.instructions().get(nextElseIdx);
                            resetToDepth(nextElse.depth());
                            if (nextElseIdx > i) {
                                i = nextElseIdx;
                            }
                        } else { // jump to the function END
                            i = body.instructions().size() - 1;
                        }
                        break;
                    }
                case BR:
                    {
                        // TODO: refactor BRs implementations
                        var targetIdx = op.labelTrue();
                        if (targetIdx == null) {
                            throw new InvalidException("unknown label");
                        }
                        var targetInstruction = body.instructions().get(targetIdx);
                        var targetDepth = targetInstruction.depth();

                        // TODO: better verify
                        // jump to loops return to the top of the loop instead
                        if (body.instructions().get(targetIdx - 1).opcode() == OpCode.LOOP) {
                            targetDepth =
                                    jumpToNextEnd(
                                            body.instructions(),
                                            body.instructions().get(targetIdx - 1),
                                            i);
                        }

                        //                        TODO: implement me properly!
                        //                            validateReturns(
                        //                                    returns.get(targetDepth),
                        // prevStack.get(targetDepth).size());

                        // if we are in an IF we should validate the other branch
                        var nextElseIdx = jumpToNextElse(body.instructions(), op, i);
                        if (nextElseIdx != -1) {
                            var nextElse = body.instructions().get(nextElseIdx);
                            resetToDepth(nextElse.depth());
                            if (nextElseIdx > i) {
                                i = nextElseIdx;
                            }
                        } else { // jump to function END
                            // the remaining instructions are not going to be evaluated ever
                            resetToDepth(targetDepth);
                            if ((targetIdx - 1) > i) {
                                i = (targetIdx - 1);
                            }
                        }
                        break;
                    }
                case BR_IF:
                    {
                        popAndVerifyType(ValueType.I32);

                        var targetIdx = op.labelTrue();
                        if (targetIdx == null) {
                            throw new InvalidException("unknown label");
                        }
                        var targetInstruction = body.instructions().get(targetIdx);
                        var targetDepth = targetInstruction.depth();

                        //                        TODO: implement me properly
                        //                        validateReturns(
                        //                                returns.get(targetDepth),
                        // prevStack.get(targetDepth).size());

                        // evaluate anyhow the next instruction to validate if the jump doesn't
                        // happen
                        break;
                    }
                case BR_TABLE:
                    {
                        // TODO: implement me
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case END:
                    {
                        if (op.scope().opcode() == OpCode.IF) {
                            valueTypeStack = pop(prevStack);
                        } else {
                            var expected = pop(returns);
                            var restoreStack = pop(prevStack);

                            validateReturns(expected, restoreStack.size());
                            if (returns.isEmpty()
                                    && expected.isEmpty()
                                    && !valueTypeStack.isEmpty()) {
                                throw new InvalidException(
                                        "type mismatch, leftovers on the stack after last end");
                            }

                            valueTypeStack = restoreStack;
                            // need to push on the stack the results
                            for (var ret : expected) {
                                push(valueTypeStack, ret);
                            }
                        }
                        break;
                    }
                default:
                    break;
            }

            switch (op.opcode()) {
                case MEMORY_COPY:
                    validateMemory(instance, (int) op.operands()[0]);
                    validateMemory(instance, (int) op.operands()[1]);
                    break;
                case MEMORY_FILL:
                    validateMemory(instance, (int) op.operands()[0]);
                    break;
                case MEMORY_INIT:
                    validateMemory(instance, (int) op.operands()[1], (int) op.operands()[0]);
                    break;
                case MEMORY_SIZE:
                case MEMORY_GROW:
                case I32_LOAD:
                case I32_LOAD8_U:
                case I32_LOAD8_S:
                case I32_LOAD16_U:
                case I32_LOAD16_S:
                case I64_LOAD:
                case I64_LOAD8_S:
                case I64_LOAD8_U:
                case I64_LOAD16_S:
                case I64_LOAD16_U:
                case I64_LOAD32_S:
                case I64_LOAD32_U:
                case F32_LOAD:
                case F64_LOAD:
                case I32_STORE:
                case I32_STORE8:
                case I32_STORE16:
                case I64_STORE:
                case I64_STORE8:
                case I64_STORE16:
                case I64_STORE32:
                case F32_STORE:
                case F64_STORE:
                    validateMemory(instance, 0);
                    break;
                default:
                    break;
            }

            switch (op.opcode()) {
                case NOP:
                case UNREACHABLE:
                case LOOP:
                case BLOCK:
                case IF:
                case ELSE:
                case RETURN:
                case BR_IF:
                case BR_TABLE:
                case BR:
                case END:
                    break;
                case DATA_DROP:
                    {
                        var index = (int) op.operands()[0];
                        if (instance.memory() == null
                                || instance.memory().dataSegments() == null
                                || index >= instance.memory().dataSegments().length) {
                            throw new InvalidException("unknown data segment");
                        }
                        break;
                    }
                case DROP:
                    {
                        popAndVerifyType(null);
                        break;
                    }
                case I32_STORE:
                case I32_STORE8:
                case I32_STORE16:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case I32_LOAD:
                case I32_LOAD8_U:
                case I32_LOAD8_S:
                case I32_LOAD16_U:
                case I32_LOAD16_S:
                case I32_CLZ:
                case I32_CTZ:
                case I32_POPCNT:
                case I32_EXTEND_8_S:
                case I32_EXTEND_16_S:
                case I32_EQZ:
                case MEMORY_GROW:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I32_CONST:
                case MEMORY_SIZE:
                case TABLE_SIZE:
                    {
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I32_ADD:
                case I32_SUB:
                case I32_MUL:
                case I32_DIV_S:
                case I32_DIV_U:
                case I32_REM_S:
                case I32_REM_U:
                case I32_AND:
                case I32_OR:
                case I32_XOR:
                case I32_EQ:
                case I32_NE:
                case I32_LT_S:
                case I32_LT_U:
                case I32_LE_S:
                case I32_LE_U:
                case I32_GT_S:
                case I32_GT_U:
                case I32_GE_S:
                case I32_GE_U:
                case I32_SHL:
                case I32_SHR_U:
                case I32_SHR_S:
                case I32_ROTL:
                case I32_ROTR:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I32_WRAP_I64:
                case I64_EQZ:
                    {
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F32_S:
                case I32_TRUNC_F32_U:
                case I32_TRUNC_SAT_F32_S:
                case I32_TRUNC_SAT_F32_U:
                case I32_REINTERPRET_F32:
                    {
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F64_S:
                case I32_TRUNC_F64_U:
                case I32_TRUNC_SAT_F64_S:
                case I32_TRUNC_SAT_F64_U:
                    {
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I64_LOAD:
                case I64_LOAD8_S:
                case I64_LOAD8_U:
                case I64_LOAD16_S:
                case I64_LOAD16_U:
                case I64_LOAD32_S:
                case I64_LOAD32_U:
                case I64_EXTEND_I32_U:
                case I64_EXTEND_I32_S:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case I64_CONST:
                    {
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case I64_STORE:
                case I64_STORE8:
                case I64_STORE16:
                case I64_STORE32:
                    {
                        popAndVerifyType(ValueType.I64);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case I64_ADD:
                case I64_SUB:
                case I64_MUL:
                case I64_DIV_S:
                case I64_DIV_U:
                case I64_REM_S:
                case I64_REM_U:
                case I64_AND:
                case I64_OR:
                case I64_XOR:
                case I64_SHL:
                case I64_SHR_U:
                case I64_SHR_S:
                case I64_ROTL:
                case I64_ROTR:
                    {
                        popAndVerifyType(ValueType.I64);
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case I64_EQ:
                case I64_NE:
                case I64_LT_S:
                case I64_LT_U:
                case I64_LE_S:
                case I64_LE_U:
                case I64_GT_S:
                case I64_GT_U:
                case I64_GE_S:
                case I64_GE_U:
                    {
                        popAndVerifyType(ValueType.I64);
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case I64_CLZ:
                case I64_CTZ:
                case I64_POPCNT:
                case I64_EXTEND_8_S:
                case I64_EXTEND_16_S:
                case I64_EXTEND_32_S:
                    {
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case I64_REINTERPRET_F64:
                case I64_TRUNC_F64_S:
                case I64_TRUNC_F64_U:
                case I64_TRUNC_SAT_F64_S:
                case I64_TRUNC_SAT_F64_U:
                    {
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case I64_TRUNC_F32_S:
                case I64_TRUNC_F32_U:
                case I64_TRUNC_SAT_F32_S:
                case I64_TRUNC_SAT_F32_U:
                    {
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.I64);
                        break;
                    }
                case F32_STORE:
                    {
                        popAndVerifyType(ValueType.F32);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case F32_CONST:
                    {
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F32_LOAD:
                case F32_CONVERT_I32_S:
                case F32_CONVERT_I32_U:
                case F32_REINTERPRET_I32:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F32_CONVERT_I64_S:
                case F32_CONVERT_I64_U:
                    {
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F64_LOAD:
                case F64_CONVERT_I32_S:
                case F64_CONVERT_I32_U:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F64_CONVERT_I64_S:
                case F64_CONVERT_I64_U:
                case F64_REINTERPRET_I64:
                    {
                        popAndVerifyType(ValueType.I64);
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F64_PROMOTE_F32:
                    {
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F32_DEMOTE_F64:
                    {
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F32_SQRT:
                case F32_ABS:
                case F32_NEG:
                case F32_CEIL:
                case F32_FLOOR:
                case F32_TRUNC:
                case F32_NEAREST:
                    {
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F32_ADD:
                case F32_SUB:
                case F32_MUL:
                case F32_DIV:
                case F32_MIN:
                case F32_MAX:
                case F32_COPYSIGN:
                    {
                        popAndVerifyType(ValueType.F32);
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.F32);
                        break;
                    }
                case F32_EQ:
                case F32_NE:
                case F32_LT:
                case F32_LE:
                case F32_GT:
                case F32_GE:
                    {
                        popAndVerifyType(ValueType.F32);
                        popAndVerifyType(ValueType.F32);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case F64_STORE:
                    {
                        popAndVerifyType(ValueType.F64);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case F64_CONST:
                    {
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F64_SQRT:
                case F64_ABS:
                case F64_NEG:
                case F64_CEIL:
                case F64_FLOOR:
                case F64_TRUNC:
                case F64_NEAREST:
                    {
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F64_ADD:
                case F64_SUB:
                case F64_MUL:
                case F64_DIV:
                case F64_MIN:
                case F64_MAX:
                case F64_COPYSIGN:
                    {
                        popAndVerifyType(ValueType.F64);
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.F64);
                        break;
                    }
                case F64_EQ:
                case F64_NE:
                case F64_LT:
                case F64_LE:
                case F64_GT:
                case F64_GE:
                    {
                        popAndVerifyType(ValueType.F64);
                        popAndVerifyType(ValueType.F64);
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case LOCAL_SET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        popAndVerifyType(expectedType);
                        break;
                    }
                case LOCAL_GET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        push(valueTypeStack, expectedType);
                        break;
                    }
                case LOCAL_TEE:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        popAndVerifyType(expectedType);
                        push(valueTypeStack, expectedType);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var type = instance.readGlobal((int) op.operands()[0]).type();
                        push(valueTypeStack, type);
                        break;
                    }
                case GLOBAL_SET:
                    {
                        popAndVerifyType(instance.readGlobal((int) op.operands()[0]).type());
                        break;
                    }
                case CALL:
                    {
                        var index = (int) op.operands()[0];
                        var types = instance.type(instance.functionType(index));
                        for (int j = types.params().size() - 1; j >= 0; j--) {
                            popAndVerifyType(types.params().get(j));
                        }
                        // TODO: verify the order
                        for (var resultType : types.returns()) {
                            push(valueTypeStack, resultType);
                        }
                        break;
                    }
                case CALL_INDIRECT:
                    {
                        var typeId = (int) op.operands()[0];
                        popAndVerifyType(ValueType.I32);

                        var types = instance.type(typeId);
                        for (int j = types.params().size() - 1; j >= 0; j--) {
                            popAndVerifyType(types.params().get(j));
                        }
                        // TODO: verify the order
                        for (var resultType : types.returns()) {
                            push(valueTypeStack, resultType);
                        }
                        break;
                    }
                case REF_NULL:
                    {
                        push(valueTypeStack, ValueType.forId((int) op.operands()[0]));
                        break;
                    }
                case REF_IS_NULL:
                    {
                        var ref = pop(valueTypeStack);
                        if (!ref.isReference()) {
                            throw new InvalidException(
                                    "type mismatch: expected FuncRef or ExtRef, but was " + ref);
                        }
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case REF_FUNC:
                    {
                        push(valueTypeStack, ValueType.FuncRef);
                        break;
                    }
                case SELECT:
                    {
                        popAndVerifyType(ValueType.I32);
                        var a = pop(valueTypeStack);
                        var b = pop(valueTypeStack);
                        // the result is polymorphic
                        push(valueTypeStack, ValueType.UNKNOWN);
                        break;
                    }
                case SELECT_T:
                    {
                        popAndVerifyType(ValueType.I32);
                        var a = pop(valueTypeStack);
                        var b = pop(valueTypeStack);

                        if (a != b) {
                            throw new InvalidException(
                                    "type mismatch: expected " + a + ", but was " + b);
                        }
                        push(valueTypeStack, a);
                        break;
                    }
                case MEMORY_COPY:
                case MEMORY_FILL:
                case MEMORY_INIT:
                case TABLE_COPY:
                case TABLE_INIT:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case TABLE_FILL:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(instance.table((int) op.operands()[0]).elementType());
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case TABLE_GET:
                    {
                        popAndVerifyType(ValueType.I32);
                        push(valueTypeStack, instance.table((int) op.operands()[0]).elementType());
                        break;
                    }
                case TABLE_SET:
                    {
                        popAndVerifyType(instance.table((int) op.operands()[0]).elementType());
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                case TABLE_GROW:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(instance.table((int) op.operands()[0]).elementType());
                        push(valueTypeStack, ValueType.I32);
                        break;
                    }
                case ELEM_DROP:
                    {
                        break;
                    }
                default:
                    throw new IllegalArgumentException(
                            "Missing type validation opcode handling for " + op.opcode());
            }
        }
    }
}
