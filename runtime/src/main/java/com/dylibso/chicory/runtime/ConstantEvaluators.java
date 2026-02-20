package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.OpCode.GLOBAL_GET;

import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public final class ConstantEvaluators {
    private ConstantEvaluators() {}

    public static long[] computeConstantValue(Instance instance, Instruction[] expr) {
        return computeConstantValue(instance, Arrays.asList(expr));
    }

    public static long[] computeConstantValue(Instance instance, List<Instruction> expr) {
        var stack = new ArrayDeque<long[]>();
        for (var instruction : expr) {
            switch (instruction.opcode()) {
                case I32_ADD:
                    {
                        var x = (int) stack.pop()[0];
                        var y = (int) stack.pop()[0];
                        stack.push(new long[] {x + y});
                        break;
                    }
                case I32_SUB:
                    {
                        var x = (int) stack.pop()[0];
                        var y = (int) stack.pop()[0];
                        stack.push(new long[] {y - x});
                        break;
                    }
                case I32_MUL:
                    {
                        var x = (int) stack.pop()[0];
                        var y = (int) stack.pop()[0];
                        int res = x * y;
                        stack.push(new long[] {res});
                        break;
                    }
                case I64_ADD:
                    {
                        var x = stack.pop()[0];
                        var y = stack.pop()[0];
                        stack.push(new long[] {x + y});
                        break;
                    }
                case I64_SUB:
                    {
                        var x = stack.pop()[0];
                        var y = stack.pop()[0];
                        stack.push(new long[] {y - x});
                        break;
                    }
                case I64_MUL:
                    {
                        var x = stack.pop()[0];
                        var y = stack.pop()[0];
                        stack.push(new long[] {x * y});
                        break;
                    }
                case V128_CONST:
                    {
                        stack.push(new long[] {instruction.operand(0), instruction.operand(1)});
                        break;
                    }
                case F32_CONST:
                case F64_CONST:
                case I32_CONST:
                case I64_CONST:
                case REF_FUNC:
                    {
                        stack.push(new long[] {instruction.operand(0)});
                        break;
                    }
                case REF_NULL:
                    {
                        stack.push(new long[] {Value.REF_NULL_VALUE});
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var idx = (int) instruction.operand(0);
                        var global = instance.global(idx);
                        if (global == null) {
                            throw new InvalidException("unknown global");
                        }
                        if (global.getType().equals(ValType.V128)) {
                            stack.push(new long[] {global.getValueLow(), global.getValueHigh()});
                        } else {
                            stack.push(new long[] {global.getValueLow()});
                        }
                        break;
                    }
                case REF_I31:
                    {
                        var val = (int) stack.pop()[0];
                        stack.push(new long[] {Value.encodeI31(val)});
                        break;
                    }
                case STRUCT_NEW:
                    {
                        var typeIdx = (int) instruction.operand(0);
                        var structType =
                                instance.module()
                                        .typeSection()
                                        .getSubType(typeIdx)
                                        .compType()
                                        .structType();
                        var fieldCount = structType.fieldTypes().length;
                        var fields = new long[fieldCount];
                        for (int i = fieldCount - 1; i >= 0; i--) {
                            fields[i] = stack.pop()[0];
                        }
                        var struct = new WasmStruct(typeIdx, fields);
                        var refId = instance.registerGcRef(struct);
                        stack.push(new long[] {refId});
                        break;
                    }
                case STRUCT_NEW_DEFAULT:
                    {
                        var typeIdx = (int) instruction.operand(0);
                        var structType =
                                instance.module()
                                        .typeSection()
                                        .getSubType(typeIdx)
                                        .compType()
                                        .structType();
                        var fieldCount = structType.fieldTypes().length;
                        var fields = new long[fieldCount];
                        for (int i = 0; i < fieldCount; i++) {
                            var ft = structType.fieldTypes()[i];
                            if (ft.storageType().valType() != null
                                    && ft.storageType().valType().isReference()) {
                                fields[i] = Value.REF_NULL_VALUE;
                            }
                        }
                        var struct = new WasmStruct(typeIdx, fields);
                        var refId = instance.registerGcRef(struct);
                        stack.push(new long[] {refId});
                        break;
                    }
                case ARRAY_NEW:
                    {
                        var typeIdx = (int) instruction.operand(0);
                        var len = (int) stack.pop()[0];
                        var fillVal = stack.pop()[0];
                        var elements = new long[len];
                        Arrays.fill(elements, fillVal);
                        var array = new WasmArray(typeIdx, elements);
                        var refId = instance.registerGcRef(array);
                        stack.push(new long[] {refId});
                        break;
                    }
                case ARRAY_NEW_DEFAULT:
                    {
                        var typeIdx = (int) instruction.operand(0);
                        var len = (int) stack.pop()[0];
                        var at =
                                instance.module()
                                        .typeSection()
                                        .getSubType(typeIdx)
                                        .compType()
                                        .arrayType();
                        var elements = new long[len];
                        if (at.fieldType().storageType().valType() != null
                                && at.fieldType().storageType().valType().isReference()) {
                            Arrays.fill(elements, Value.REF_NULL_VALUE);
                        }
                        var array = new WasmArray(typeIdx, elements);
                        var refId = instance.registerGcRef(array);
                        stack.push(new long[] {refId});
                        break;
                    }
                case ARRAY_NEW_FIXED:
                    {
                        var typeIdx = (int) instruction.operand(0);
                        var len = (int) instruction.operand(1);
                        var elements = new long[len];
                        for (int i = len - 1; i >= 0; i--) {
                            elements[i] = stack.pop()[0];
                        }
                        var array = new WasmArray(typeIdx, elements);
                        var refId = instance.registerGcRef(array);
                        stack.push(new long[] {refId});
                        break;
                    }
                case ANY_CONVERT_EXTERN:
                case EXTERN_CONVERT_ANY:
                    {
                        // Identity operations at runtime
                        break;
                    }
                case END:
                    {
                        break;
                    }
                default:
                    throw new MalformedException(
                            "Invalid instruction in constant value" + instruction);
            }
        }

        return stack.pop();
    }

    public static Instance computeConstantInstance(Instance instance, List<Instruction> expr) {
        for (Instruction instruction : expr) {
            if (instruction.opcode() == GLOBAL_GET) {
                return instance.global((int) instruction.operand(0)).getInstance();
            }
        }
        return instance;
    }
}
