package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.OpCode.GLOBAL_GET;

import com.dylibso.chicory.wasm.MalformedException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

final class ConstantEvaluators {
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
                        if (instance.global(idx).getType().equals(ValType.V128)) {
                            stack.push(
                                    new long[] {
                                        instance.global(idx).getValueLow(),
                                        instance.global(idx).getValueHigh()
                                    });
                        } else {
                            stack.push(new long[] {instance.global(idx).getValueLow()});
                        }
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
