package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.OpCode.GLOBAL_GET;

import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;

final class ConstantEvaluators {
    private ConstantEvaluators() {}

    public static long computeConstantValue(Instance instance, Instruction[] expr) {
        return computeConstantValue(instance, Arrays.asList(expr));
    }

    public static long computeConstantValue(Instance instance, List<Instruction> expr) {
        long tos = -1L;
        for (var instruction : expr) {
            switch (instruction.opcode()) {
                case F32_CONST:
                case F64_CONST:
                case I32_CONST:
                case I64_CONST:
                case REF_FUNC:
                    {
                        tos = instruction.operand(0);
                        break;
                    }
                case REF_NULL:
                    {
                        tos = Value.REF_NULL_VALUE;
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var idx = (int) instruction.operand(0);
                        tos = instance.global(idx).getValue();
                        break;
                    }
                case END:
                    {
                        break;
                    }
            }
        }
        return tos;
    }

    public static ValueType computeConstantType(Instance instance, List<Instruction> expr) {
        ValueType tos = null;
        for (var instruction : expr) {
            switch (instruction.opcode()) {
                case F32_CONST:
                    {
                        tos = ValueType.F32;
                        break;
                    }
                case F64_CONST:
                    {
                        tos = ValueType.F64;
                        break;
                    }
                case I32_CONST:
                    {
                        tos = ValueType.I32;
                        break;
                    }
                case I64_CONST:
                    {
                        tos = ValueType.I64;
                        break;
                    }
                case REF_NULL:
                    {
                        ValueType vt = ValueType.refTypeForId((int) instruction.operand(0));
                        if (vt == ValueType.ExternRef) {
                            tos = ValueType.ExternRef;
                        } else if (vt == ValueType.FuncRef) {
                            tos = ValueType.FuncRef;
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
                        tos = ValueType.FuncRef;
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
                            tos = instance.imports().global(idx).instance().getType();
                        } else {
                            throw new InvalidException(
                                    "unknown global "
                                            + idx
                                            + ", initializer expression can only reference"
                                            + " an imported global");
                        }
                        break;
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
