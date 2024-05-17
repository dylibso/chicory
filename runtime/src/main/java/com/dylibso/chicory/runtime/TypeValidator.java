package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// Heavily inspired by wazero
// https://github.com/tetratelabs/wazero/blob/5a8a053bff0ae795b264de9672016745cb842070/internal/wasm/func_validation.go
public class TypeValidator {

    private Deque<ValueType> valueTypeStack = new ArrayDeque<>();
    private Deque<Integer> stackLimit = new ArrayDeque<>();
    private Deque<List<ValueType>> returns = new ArrayDeque<>();

    private void popAndVerifyType(ValueType expected) {
        var have = (valueTypeStack.size() > stackLimit.peek()) ? valueTypeStack.poll() : null;
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

    public void validate(FunctionBody body, FunctionType functionType, Instance instance) {
        var localTypes = body.localTypes();
        var inputLen = functionType.params().size();
        stackLimit.push(0);
        returns.push(functionType.returns());

        for (var i = 0; i < body.instructions().size(); i++) {
            var op = body.instructions().get(i);

            // memory validation
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
                    break;
                case BR:
                case BR_IF:
                case BR_TABLE:
                case RETURN:
                    {
                        // TODO: verify is it's needed to implement a polymorphic stack?
                        // TODO: verify this logic not completely convinced about it
                        var nextEndOffset = 1;
                        var tmpOp = body.instructions().get(i + nextEndOffset);
                        while (tmpOp.opcode() != OpCode.END) {
                            nextEndOffset++;
                            tmpOp = body.instructions().get(i + nextEndOffset);
                        }
                        // go to the next END
                        i = i + nextEndOffset;

                        var rets =
                                (op.opcode() == OpCode.RETURN)
                                        ? functionType.returns()
                                        : returns.peek();
                        for (var expected : rets) {
                            popAndVerifyType(expected);
                        }
                        for (var expected : rets) {
                            valueTypeStack.push(expected);
                        }
                        break;
                    }
                case IF:
                case ELSE:
                case LOOP:
                case BLOCK:
                    {
                        returns.push(List.of());
                        stackLimit.push(valueTypeStack.size());
                        break;
                    }
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
                case END:
                    {
                        for (var expected : returns.pop()) {
                            popAndVerifyType(expected);
                        }
                        stackLimit.pop();

                        // last return
                        if (stackLimit.isEmpty() && !valueTypeStack.isEmpty()) {
                            throw new InvalidException("type mismatch: expected [], but was [...]");
                        }
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
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case I32_CONST:
                case MEMORY_SIZE:
                    {
                        valueTypeStack.push(ValueType.I32);
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
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case I32_WRAP_I64:
                case I64_EQZ:
                    {
                        popAndVerifyType(ValueType.I64);
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F32_S:
                case I32_TRUNC_F32_U:
                case I32_TRUNC_SAT_F32_S:
                case I32_TRUNC_SAT_F32_U:
                case I32_REINTERPRET_F32:
                    {
                        popAndVerifyType(ValueType.F32);
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F64_S:
                case I32_TRUNC_F64_U:
                case I32_TRUNC_SAT_F64_S:
                case I32_TRUNC_SAT_F64_U:
                    {
                        popAndVerifyType(ValueType.F64);
                        valueTypeStack.push(ValueType.I32);
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
                        valueTypeStack.push(ValueType.I64);
                        break;
                    }
                case I64_CONST:
                    {
                        valueTypeStack.push(ValueType.I64);
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
                        valueTypeStack.push(ValueType.I64);
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
                        valueTypeStack.push(ValueType.I32);
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
                        valueTypeStack.push(ValueType.I64);
                        break;
                    }
                case I64_REINTERPRET_F64:
                case I64_TRUNC_F64_S:
                case I64_TRUNC_F64_U:
                case I64_TRUNC_SAT_F64_S:
                case I64_TRUNC_SAT_F64_U:
                    {
                        popAndVerifyType(ValueType.F64);
                        valueTypeStack.push(ValueType.I64);
                        break;
                    }
                case I64_TRUNC_F32_S:
                case I64_TRUNC_F32_U:
                case I64_TRUNC_SAT_F32_S:
                case I64_TRUNC_SAT_F32_U:
                    {
                        popAndVerifyType(ValueType.F32);
                        valueTypeStack.push(ValueType.I64);
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
                        valueTypeStack.push(ValueType.F32);
                        break;
                    }
                case F32_LOAD:
                case F32_CONVERT_I32_S:
                case F32_CONVERT_I32_U:
                case F32_REINTERPRET_I32:
                    {
                        popAndVerifyType(ValueType.I32);
                        valueTypeStack.push(ValueType.F32);
                        break;
                    }
                case F32_CONVERT_I64_S:
                case F32_CONVERT_I64_U:
                    {
                        popAndVerifyType(ValueType.I64);
                        valueTypeStack.push(ValueType.F32);
                        break;
                    }
                case F64_LOAD:
                case F64_CONVERT_I32_S:
                case F64_CONVERT_I32_U:
                    {
                        popAndVerifyType(ValueType.I32);
                        valueTypeStack.push(ValueType.F64);
                        break;
                    }
                case F64_CONVERT_I64_S:
                case F64_CONVERT_I64_U:
                case F64_REINTERPRET_I64:
                    {
                        popAndVerifyType(ValueType.I64);
                        valueTypeStack.push(ValueType.F64);
                        break;
                    }
                case F64_PROMOTE_F32:
                    {
                        popAndVerifyType(ValueType.F32);
                        valueTypeStack.push(ValueType.F64);
                        break;
                    }
                case F32_DEMOTE_F64:
                    {
                        popAndVerifyType(ValueType.F64);
                        valueTypeStack.push(ValueType.F32);
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
                        valueTypeStack.push(ValueType.F32);
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
                        valueTypeStack.push(ValueType.F32);
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
                        valueTypeStack.push(ValueType.I32);
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
                        valueTypeStack.push(ValueType.F64);
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
                        valueTypeStack.push(ValueType.F64);
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
                        valueTypeStack.push(ValueType.F64);
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
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case LOCAL_SET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : localTypes.get(index - inputLen);
                        popAndVerifyType(expectedType);
                        break;
                    }
                case LOCAL_GET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : localTypes.get(index - inputLen);
                        valueTypeStack.push(expectedType);
                        break;
                    }
                case LOCAL_TEE:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : localTypes.get(index - inputLen);
                        popAndVerifyType(expectedType);
                        valueTypeStack.push(expectedType);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var type = instance.readGlobal((int) op.operands()[0]).type();
                        valueTypeStack.push(type);
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
                            valueTypeStack.push(resultType);
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
                            valueTypeStack.push(resultType);
                        }
                        break;
                    }
                case REF_NULL:
                    {
                        valueTypeStack.push(ValueType.forId((int) op.operands()[0]));
                        break;
                    }
                case REF_IS_NULL:
                    {
                        var ref = valueTypeStack.poll();
                        if (!ref.isReference()) {
                            throw new InvalidException(
                                    "type mismatch: expected FuncRef or ExtRef, but was " + ref);
                        }
                        valueTypeStack.push(ValueType.I32);
                        break;
                    }
                case SELECT:
                    {
                        popAndVerifyType(ValueType.I32);
                        var a = valueTypeStack.poll();
                        var b = valueTypeStack.poll();
                        // the result is polymorphic
                        valueTypeStack.push(ValueType.UNKNOWN);
                        break;
                    }
                case SELECT_T:
                    {
                        popAndVerifyType(ValueType.I32);
                        var a = valueTypeStack.poll();
                        var b = valueTypeStack.poll();

                        if (a != b) {
                            throw new InvalidException(
                                    "type mismatch: expected " + a + ", but was " + b);
                        }
                        valueTypeStack.push(a);
                        break;
                    }
                case MEMORY_COPY:
                case MEMORY_FILL:
                case MEMORY_INIT:
                    {
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        popAndVerifyType(ValueType.I32);
                        break;
                    }
                default:
                    throw new IllegalArgumentException(
                            "Missing type validation opcode handling for " + op.opcode());
            }
        }
    }
}
