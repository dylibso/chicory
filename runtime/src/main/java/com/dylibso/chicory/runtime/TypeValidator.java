package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Heavily inspired by wazero
// https://github.com/tetratelabs/wazero/blob/5a8a053bff0ae795b264de9672016745cb842070/internal/wasm/func_validation.go
// control flow implementation follows:
// https://webassembly.github.io/spec/core/appendix/algorithm.html
public class TypeValidator {

    private boolean isNum(ValueType t) {
        return t.isNumeric() || t == ValueType.UNKNOWN;
    }

    private boolean isRef(ValueType t) {
        return t.isReference() || t == ValueType.UNKNOWN;
    }

    private static class CtrlFrame {
        private final OpCode opCode;
        private final List<ValueType> startTypes;
        private final List<ValueType> endTypes;
        private final int height;
        private boolean unreachable;
        private boolean hasElse;

        public CtrlFrame(
                OpCode opCode,
                List<ValueType> startTypes,
                List<ValueType> endTypes,
                int height,
                boolean unreachable,
                boolean hasElse) {
            this.opCode = opCode;
            this.startTypes = startTypes;
            this.endTypes = endTypes;
            this.height = height;
            this.unreachable = unreachable;
            this.hasElse = hasElse;
        }
    }

    private List<ValueType> valueTypeStack = new ArrayList<>();
    private List<CtrlFrame> ctrlFrameStack = new ArrayList<>();

    private void pushVal(ValueType valType) {
        valueTypeStack.add(valType);
    }

    private ValueType popVal() {
        var frame = ctrlFrameStack.get(ctrlFrameStack.size() - 1);
        if (valueTypeStack.size() == frame.height && frame.unreachable) {
            return ValueType.UNKNOWN;
        }
        if (valueTypeStack.size() == frame.height) {
            throw new InvalidException("type mismatch, popVal()");
        }
        return valueTypeStack.remove(valueTypeStack.size() - 1);
    }

    private ValueType popVal(ValueType expected) {
        var actual = popVal();
        if (actual != expected && actual != ValueType.UNKNOWN && expected != ValueType.UNKNOWN) {
            throw new InvalidException("type mismatch, popVal(expected)");
        }
        return actual;
    }

    private void pushVals(List<ValueType> valTypes) {
        for (var t : valTypes) {
            pushVal(t);
        }
    }

    private List<ValueType> popVals(List<ValueType> valTypes) {
        var popped = new ValueType[valTypes.size()];
        for (int i = 0; i < valTypes.size(); i++) {
            popped[i] = popVal(valTypes.get(valTypes.size() - 1 - i));
        }
        return Arrays.asList(popped);
    }

    private void pushCtrl(OpCode opCode, List<ValueType> in, List<ValueType> out) {
        var frame = new CtrlFrame(opCode, in, out, valueTypeStack.size(), false, false);
        ctrlFrameStack.add(frame);
        pushVals(in);
    }

    private CtrlFrame popCtrl() {
        if (ctrlFrameStack.isEmpty()) {
            throw new InvalidException("type mismatch, control frame stack empty");
        }
        var frame = ctrlFrameStack.get(ctrlFrameStack.size() - 1);
        popVals(frame.endTypes);
        if (valueTypeStack.size() != frame.height) {
            throw new InvalidException("type mismatch, wrong stack height");
        }
        ctrlFrameStack.remove(ctrlFrameStack.size() - 1);
        return frame;
    }

    private List<ValueType> labelTypes(CtrlFrame frame) {
        if (frame.opCode == OpCode.LOOP) {
            return frame.startTypes;
        } else {
            return frame.endTypes;
        }
    }

    private void resetAtStackLimit() {
        var frame = ctrlFrameStack.get(ctrlFrameStack.size() - 1);
        while (valueTypeStack.size() > frame.height) {
            valueTypeStack.remove(valueTypeStack.size() - 1);
        }
    }

    private void unreachable() {
        var frame = ctrlFrameStack.get(ctrlFrameStack.size() - 1);
        resetAtStackLimit();
        frame.unreachable = true;
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

    private List<ValueType> getReturns(Instruction op, Instance instance) {
        var typeId = (int) op.operands()[0];
        if (typeId == 0x40) { // epsilon
            return List.of();
        } else if (ValueType.isValid(typeId)) {
            return List.of(ValueType.forId(typeId));
        } else {
            return instance.type(typeId).returns();
        }
    }

    private List<ValueType> getParams(Instruction op, Instance instance) {
        var typeId = (int) op.operands()[0];
        if (typeId == 0x40) { // epsilon
            return List.of();
        } else if (ValueType.isValid(typeId)) {
            return List.of();
        } else {
            return instance.type(typeId).params();
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
        pushCtrl(null, new ArrayList<>(), functionType.returns());

        for (var i = 0; i < body.instructions().size(); i++) {
            var op = body.instructions().get(i);

            // control flow instructions handling
            switch (op.opcode()) {
                case UNREACHABLE:
                    {
                        unreachable();
                        break;
                    }
                case IF:
                    {
                        popVal(ValueType.I32);
                        // fallthrough
                    }
                case LOOP: // t1* -> t2*
                case BLOCK:
                    {
                        var t1 = getParams(op, instance);
                        var t2 = getReturns(op, instance);
                        popVals(t1);
                        pushCtrl(op.opcode(), t1, t2);
                        break;
                    }
                case END:
                    {
                        var frame = popCtrl();
                        if (frame.opCode == OpCode.IF && !frame.hasElse && frame.startTypes.size() != frame.endTypes.size()) {
                            throw new InvalidException("type mismatch, unbalanced if branches");
                        }
                        pushVals(frame.endTypes);
                        break;
                    }
                case ELSE:
                    {
                        var frame = popCtrl();
                        if (frame.opCode != OpCode.IF) {
                            throw new InvalidException("else doesn't belong to if");
                        }
                        pushCtrl(op.opcode(), frame.startTypes, frame.endTypes);
                        ctrlFrameStack.get(ctrlFrameStack.size() - 1).hasElse = true;
                        break;
                    }
                case BR:
                    {
                        if (op.labelTrue() == null) {
                            throw new InvalidException("unknown label");
                        }
                        var n = (int) op.operands()[0];
                        if (ctrlFrameStack.size() < n) {
                            throw new InvalidException("verify me");
                        }
                        try {
                            popVals(labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - n)));
                        } catch (Exception e) {
                            System.out.println("debug");
                        }
                        unreachable();
                        break;
                    }
                case BR_IF:
                    {
                        if (op.labelTrue() == null) {
                            throw new InvalidException("unknown label");
                        }
                        popVal(ValueType.I32);
                        var n = (int) op.operands()[0];
                        if (ctrlFrameStack.size() < n) {
                            throw new InvalidException("verify me");
                        }
                        popVals(labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - n)));
                        pushVals(labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - n)));
                        break;
                    }
                case BR_TABLE:
                    {
                        popVal(ValueType.I32);
                        var m = (int) op.operands()[op.operands().length - 1];
                        if (ctrlFrameStack.size() < m) {
                            throw new InvalidException("verify me");
                        }
                        var arity =
                                labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - m))
                                        .size();
                        for (var idx = 1; idx < op.operands().length - 2; idx++) {
                            var n = (int) op.operands()[idx];
                            if (ctrlFrameStack.size() < n) {
                                throw new InvalidException("verify me");
                            }
                            var labelTypes =
                                    labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - n));
                            if (labelTypes.size() != arity) {
                                throw new InvalidException("mismatched arity in BR_TABLE");
                            }
                            pushVals(popVals(labelTypes));
                        }
                        popVals(labelTypes(ctrlFrameStack.get(ctrlFrameStack.size() - 1 - m)));
                        unreachable();
                        break;
                    }
                case RETURN:
                    {
                        popVals(labelTypes(ctrlFrameStack.get(0)));
                        unreachable();
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
                        popVal();
                        break;
                    }
                case I32_STORE:
                case I32_STORE8:
                case I32_STORE16:
                    {
                        popVal(ValueType.I32);
                        popVal(ValueType.I32);
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
                        popVal(ValueType.I32);
                        pushVal(ValueType.I32);
                        break;
                    }
                case I32_CONST:
                case MEMORY_SIZE:
                case TABLE_SIZE:
                    {
                        pushVal(ValueType.I32);
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
                        popVal(ValueType.I32);
                        popVal(ValueType.I32);
                        pushVal(ValueType.I32);
                        break;
                    }
                case I32_WRAP_I64:
                case I64_EQZ:
                    {
                        popVal(ValueType.I64);
                        pushVal(ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F32_S:
                case I32_TRUNC_F32_U:
                case I32_TRUNC_SAT_F32_S:
                case I32_TRUNC_SAT_F32_U:
                case I32_REINTERPRET_F32:
                    {
                        popVal(ValueType.F32);
                        pushVal(ValueType.I32);
                        break;
                    }
                case I32_TRUNC_F64_S:
                case I32_TRUNC_F64_U:
                case I32_TRUNC_SAT_F64_S:
                case I32_TRUNC_SAT_F64_U:
                    {
                        popVal(ValueType.F64);
                        pushVal(ValueType.I32);
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
                        popVal(ValueType.I32);
                        pushVal(ValueType.I64);
                        break;
                    }
                case I64_CONST:
                    {
                        pushVal(ValueType.I64);
                        break;
                    }
                case I64_STORE:
                case I64_STORE8:
                case I64_STORE16:
                case I64_STORE32:
                    {
                        popVal(ValueType.I64);
                        popVal(ValueType.I32);
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
                        popVal(ValueType.I64);
                        popVal(ValueType.I64);
                        pushVal(ValueType.I64);
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
                        popVal(ValueType.I64);
                        popVal(ValueType.I64);
                        pushVal(ValueType.I32);
                        break;
                    }
                case I64_CLZ:
                case I64_CTZ:
                case I64_POPCNT:
                case I64_EXTEND_8_S:
                case I64_EXTEND_16_S:
                case I64_EXTEND_32_S:
                    {
                        popVal(ValueType.I64);
                        pushVal(ValueType.I64);
                        break;
                    }
                case I64_REINTERPRET_F64:
                case I64_TRUNC_F64_S:
                case I64_TRUNC_F64_U:
                case I64_TRUNC_SAT_F64_S:
                case I64_TRUNC_SAT_F64_U:
                    {
                        popVal(ValueType.F64);
                        pushVal(ValueType.I64);
                        break;
                    }
                case I64_TRUNC_F32_S:
                case I64_TRUNC_F32_U:
                case I64_TRUNC_SAT_F32_S:
                case I64_TRUNC_SAT_F32_U:
                    {
                        popVal(ValueType.F32);
                        pushVal(ValueType.I64);
                        break;
                    }
                case F32_STORE:
                    {
                        popVal(ValueType.F32);
                        popVal(ValueType.I32);
                        break;
                    }
                case F32_CONST:
                    {
                        pushVal(ValueType.F32);
                        break;
                    }
                case F32_LOAD:
                case F32_CONVERT_I32_S:
                case F32_CONVERT_I32_U:
                case F32_REINTERPRET_I32:
                    {
                        popVal(ValueType.I32);
                        pushVal(ValueType.F32);
                        break;
                    }
                case F32_CONVERT_I64_S:
                case F32_CONVERT_I64_U:
                    {
                        popVal(ValueType.I64);
                        pushVal(ValueType.F32);
                        break;
                    }
                case F64_LOAD:
                case F64_CONVERT_I32_S:
                case F64_CONVERT_I32_U:
                    {
                        popVal(ValueType.I32);
                        pushVal(ValueType.F64);
                        break;
                    }
                case F64_CONVERT_I64_S:
                case F64_CONVERT_I64_U:
                case F64_REINTERPRET_I64:
                    {
                        popVal(ValueType.I64);
                        pushVal(ValueType.F64);
                        break;
                    }
                case F64_PROMOTE_F32:
                    {
                        popVal(ValueType.F32);
                        pushVal(ValueType.F64);
                        break;
                    }
                case F32_DEMOTE_F64:
                    {
                        popVal(ValueType.F64);
                        pushVal(ValueType.F32);
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
                        popVal(ValueType.F32);
                        pushVal(ValueType.F32);
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
                        popVal(ValueType.F32);
                        popVal(ValueType.F32);
                        pushVal(ValueType.F32);
                        break;
                    }
                case F32_EQ:
                case F32_NE:
                case F32_LT:
                case F32_LE:
                case F32_GT:
                case F32_GE:
                    {
                        popVal(ValueType.F32);
                        popVal(ValueType.F32);
                        pushVal(ValueType.I32);
                        break;
                    }
                case F64_STORE:
                    {
                        popVal(ValueType.F64);
                        popVal(ValueType.I32);
                        break;
                    }
                case F64_CONST:
                    {
                        pushVal(ValueType.F64);
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
                        popVal(ValueType.F64);
                        pushVal(ValueType.F64);
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
                        popVal(ValueType.F64);
                        popVal(ValueType.F64);
                        pushVal(ValueType.F64);
                        break;
                    }
                case F64_EQ:
                case F64_NE:
                case F64_LT:
                case F64_LE:
                case F64_GT:
                case F64_GE:
                    {
                        popVal(ValueType.F64);
                        popVal(ValueType.F64);
                        pushVal(ValueType.I32);
                        break;
                    }
                case LOCAL_SET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        popVal(expectedType);
                        break;
                    }
                case LOCAL_GET:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        pushVal(expectedType);
                        break;
                    }
                case LOCAL_TEE:
                    {
                        var index = (int) op.operands()[0];
                        ValueType expectedType =
                                (index < inputLen)
                                        ? functionType.params().get(index)
                                        : getLocalType(localTypes, index - inputLen);
                        popVal(expectedType);
                        pushVal(expectedType);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var type = instance.readGlobal((int) op.operands()[0]).type();
                        pushVal(type);
                        break;
                    }
                case GLOBAL_SET:
                    {
                        popVal(instance.readGlobal((int) op.operands()[0]).type());
                        break;
                    }
                case CALL:
                    {
                        var index = (int) op.operands()[0];
                        var types = instance.type(instance.functionType(index));
                        for (int j = types.params().size() - 1; j >= 0; j--) {
                            popVal(types.params().get(j));
                        }
                        pushVals(types.returns());
                        break;
                    }
                case CALL_INDIRECT:
                    {
                        var typeId = (int) op.operands()[0];
                        popVal(ValueType.I32);

                        var types = instance.type(typeId);
                        for (int j = types.params().size() - 1; j >= 0; j--) {
                            popVal(types.params().get(j));
                        }
                        pushVals(types.returns());
                        break;
                    }
                case REF_NULL:
                    {
                        pushVal(ValueType.forId((int) op.operands()[0]));
                        break;
                    }
                case REF_IS_NULL:
                    {
                        var ref = popVal();
                        if (!isRef(ref)) {
                            throw new InvalidException(
                                    "type mismatch: expected FuncRef or ExtRef, but was " + ref);
                        }
                        pushVal(ValueType.I32);
                        break;
                    }
                case REF_FUNC:
                    {
                        pushVal(ValueType.FuncRef);
                        break;
                    }
                case SELECT:
                    {
                        popVal(ValueType.I32);
                        var t1 = popVal();
                        var t2 = popVal();
                        if (!(isNum(t1) && isNum(t2))) {
                            throw new InvalidException(
                                    "type mismatch: select should have numeric arguments");
                        }
                        if (t1 != t2 && t1 != ValueType.UNKNOWN && t2 != ValueType.UNKNOWN) {
                            throw new InvalidException("type mismatch: in select");
                        }
                        if (t1 == ValueType.UNKNOWN) {
                            pushVal(t2);
                        } else {
                            pushVal(t1);
                        }
                        break;
                    }
                case SELECT_T:
                    {
                        popVal(ValueType.I32);
                        var t = ValueType.forId((int) op.operands()[0]);
                        popVal(t);
                        popVal(t);
                        pushVal(t);
                        break;
                    }
                case MEMORY_COPY:
                case MEMORY_FILL:
                case MEMORY_INIT:
                case TABLE_COPY:
                case TABLE_INIT:
                    {
                        popVal(ValueType.I32);
                        popVal(ValueType.I32);
                        popVal(ValueType.I32);
                        break;
                    }
                case TABLE_FILL:
                    {
                        popVal(ValueType.I32);
                        popVal(instance.table((int) op.operands()[0]).elementType());
                        popVal(ValueType.I32);
                        break;
                    }
                case TABLE_GET:
                    {
                        popVal(ValueType.I32);
                        pushVal(instance.table((int) op.operands()[0]).elementType());
                        break;
                    }
                case TABLE_SET:
                    {
                        popVal(instance.table((int) op.operands()[0]).elementType());
                        popVal(ValueType.I32);
                        break;
                    }
                case TABLE_GROW:
                    {
                        popVal(ValueType.I32);
                        popVal(instance.table((int) op.operands()[0]).elementType());
                        pushVal(ValueType.I32);
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
