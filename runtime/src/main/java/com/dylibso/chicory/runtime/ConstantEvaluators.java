package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.OpCode.GLOBAL_GET;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;

public final class ConstantEvaluators {
    private ConstantEvaluators() {}

    public static Value computeConstantValue(Instance instance, Instruction[] expr) {
        return computeConstantValue(instance, Arrays.asList(expr));
    }

    public static Value computeConstantValue(Instance instance, List<Instruction> expr) {
        Value tos = null;
        for (var instruction : expr) {
            switch (instruction.opcode()) {
                case F32_CONST:
                    {
                        tos = Value.f32(instruction.operand(0));
                        break;
                    }
                case F64_CONST:
                    {
                        tos = Value.f64(instruction.operand(0));
                        break;
                    }
                case I32_CONST:
                    {
                        tos = Value.i32(instruction.operand(0));
                        break;
                    }
                case I64_CONST:
                    {
                        tos = Value.i64(instruction.operand(0));
                        break;
                    }
                case REF_NULL:
                    {
                        ValueType vt = ValueType.refTypeForId((int) instruction.operand(0));
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
                        var idx = (int) instruction.operand(0);
                        instance.function(idx);
                        tos = Value.funcRef(idx);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var idx = (int) instruction.operand(0);
                        if (idx < instance.imports().globalCount()) {
                            if (instance.imports().global(idx).instance().getMutabilityType()
                                    != MutabilityType.Const) {
                                throw new InvalidException(
                                        "constant expression required, initializer expression"
                                                + " cannot reference a mutable global");
                            }
                            var t = instance.imports().global(idx).instance().getType();
                            return new Value(t, instance.readGlobal(idx));
                        } else {
                            throw new InvalidException(
                                    "unknown global "
                                            + idx
                                            + ", initializer expression can only reference"
                                            + " an imported global");
                        }
                    }
                case END:
                    {
                        break;
                    }
                default:
                    {
                        throw new InvalidException(
                                "constant expression required, but non-constant instruction"
                                        + " encountered: "
                                        + instruction);
                    }
            }
        }
        if (tos == null) {
            throw new InvalidException("type mismatch, expected constant value");
        }
        return tos;
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
