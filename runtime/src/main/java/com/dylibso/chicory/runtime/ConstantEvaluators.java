package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;

public class ConstantEvaluators {
    public static Value computeConstantValue(Instance instance, Instruction[] expr) {
        return computeConstantValue(instance, Arrays.asList(expr));
    }

    public static Value computeConstantValue(Instance instance, List<Instruction> expr) {
        Value tos = null;
        for (Instruction instruction : expr) {
            switch (instruction.opcode()) {
                case F32_CONST: {
                    tos = Value.f32(instruction.operands()[0]);
                    break;
                }
                case F64_CONST: {
                    tos = Value.f64(instruction.operands()[0]);
                    break;
                }
                case I32_CONST: {
                    tos = Value.i32(instruction.operands()[0]);
                    break;
                }
                case I64_CONST: {
                    tos = Value.i64(instruction.operands()[0]);
                    break;
                }
                case REF_NULL: {
                    ValueType vt = ValueType.refTypeForId((int) instruction.operands()[0]);
                    if (vt == ValueType.ExternRef) {
                        tos = Value.EXTREF_NULL;
                    } else if (vt == ValueType.FuncRef) {
                        tos = Value.FUNCREF_NULL;
                    } else {
                        throw new IllegalStateException("Unexpected wrong type for ref.null instruction");
                    }
                    break;
                }
                case REF_FUNC: {
                    tos = Value.funcRef((int) instruction.operands()[0]);
                    break;
                }
                case GLOBAL_GET: {
                    return instance.readGlobal((int) instruction.operands()[0]);
                }
                case END: {
                    break;
                }
                default: {
                    throw new ChicoryException("Non-constant instruction encountered: " + instruction);
                }
            }
        }
        if (tos == null) {
            throw new ChicoryException("No constant value loaded");
        }
        return tos;
    }

    public static Instance computeConstantInstance(Instance instance, List<Instruction> expr) {
        for (Instruction instruction : expr) {
            switch (instruction.opcode()) {
                case GLOBAL_GET: {
                    return instance.global((int) instruction.operands()[0]).getInstance();
                }
            }
        }
        return instance;
    }
}
