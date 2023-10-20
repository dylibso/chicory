package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.*;
import java.util.*;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
public class Machine {
    private MStack stack;
    private Stack<StackFrame> callStack;
    private Instance instance;

    public Machine(Instance instance) {
        this.instance = instance;
        this.stack = new MStack();
        this.callStack = new Stack<>();
    }

    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        var func = instance.getFunction(funcId);
        if (func != null) {
            this.callStack.push(new StackFrame(funcId, 0, args, func.getLocals()));
            eval(func.getInstructions());
        } else {
            this.callStack.push(new StackFrame(funcId, 0, args, List.of()));
            var imprt = instance.getImports()[funcId];
            var hostFunc = imprt.getHandle();
            var results = hostFunc.apply(this.instance.getMemory(), args);
            // a host function can return null or an array of ints
            // which we will push onto the stack
            if (results != null) {
                for (var result : results) {
                    this.stack.push(result);
                }
            }
        }
        if (this.callStack.size() > 0) this.callStack.pop();
        if (!popResults) {
            return null;
        }

        var typeId = instance.getFunctionTypes()[funcId];
        var type = instance.getTypes()[typeId];
        if (type.getReturns().length == 0) return null;
        if (this.stack.size() == 0) return null;

        var totalResults = type.getReturns().length;
        var results = new Value[totalResults];
        for (var i = totalResults - 1; i >= 0; i--) {
            results[i] = this.stack.pop();
        }
        return results;
    }

    void eval(List<Instruction> code) throws ChicoryException {
        try {
            var frame = callStack.peek();
            boolean shouldReturn = false;

            loop:
            while (frame.pc < code.size()) {
                if (shouldReturn) return;
                var instruction = code.get(frame.pc++);
                var opcode = instruction.getOpcode();
                var operands = instruction.getOperands();
                //                System.out.println(
                //                        "func="
                //                                + frame.funcId
                //                                + "@"
                //                                + frame.pc
                //                                + ": "
                //                                + instruction
                //                                + "stack="
                //                                + this.stack);
                switch (opcode) {
                    case UNREACHABLE:
                        throw new TrapException("Trapped on unreachable instruction", callStack);
                    case NOP:
                        break;
                    case LOOP:
                    case BLOCK:
                        {
                            frame.blockDepth++;
                            break;
                        }
                    case IF:
                        {
                            frame.blockDepth++;
                            var pred = this.stack.pop().asInt();
                            if (pred == 0) {
                                frame.pc = instruction.getLabelFalse();
                            } else {
                                frame.pc = instruction.getLabelTrue();
                            }
                            break;
                        }
                    case ELSE:
                    case BR:
                        {
                            frame.pc = instruction.getLabelTrue();
                            break;
                        }
                    case BR_IF:
                        {
                            var pred = this.stack.pop().asInt();
                            if (pred == 0) {
                                frame.pc = instruction.getLabelFalse();
                            } else {
                                frame.pc = instruction.getLabelTrue();
                            }
                            break;
                        }
                    case BR_TABLE:
                        {
                            var pred = this.stack.pop().asInt();
                            if (pred < 0 || pred >= instruction.getLabelTable().length - 1) {
                                // choose default
                                frame.pc =
                                        instruction
                                                .getLabelTable()[
                                                instruction.getLabelTable().length - 1];
                            } else {
                                frame.pc = instruction.getLabelTable()[pred];
                            }
                            break;
                        }
                    case RETURN:
                        shouldReturn = true;
                        break;
                    case CALL_INDIRECT:
                        {
                            var tableIdx = operands[1];
                            if (tableIdx != 0)
                                throw new ChicoryException(
                                        "We only support a table index of 0 in call-indirect");
                            var funcTableIdx = this.stack.pop().asInt();
                            var funcId = instance.getTable().getFuncRef(funcTableIdx);
                            var typeId = (int) operands[0];
                            var type = instance.getTypes()[typeId];
                            // given a list of param types, let's pop those params off the stack
                            // and pass as args to the function call
                            var args = extractArgsForParams(type.getParams());
                            call(funcId, args, false);
                            break;
                        }
                    case DROP:
                        this.stack.pop();
                        break;
                    case SELECT:
                        {
                            var pred = this.stack.pop().asInt();
                            var b = this.stack.pop();
                            var a = this.stack.pop();
                            if (pred == 0) {
                                this.stack.push(b);
                            } else {
                                this.stack.push(a);
                            }
                            break;
                        }
                    case END:
                        {
                            // if this is the last end, then we're done with
                            // the function
                            if (frame.blockDepth == 0) {
                                break loop;
                            }
                            frame.blockDepth--;
                            break;
                        }
                    case LOCAL_GET:
                        {
                            this.stack.push(frame.getLocal((int) operands[0]));
                            break;
                        }
                    case LOCAL_SET:
                        {
                            frame.setLocal((int) operands[0], this.stack.pop());
                            break;
                        }
                    case LOCAL_TEE:
                        {
                            // here we peek instead of pop, leaving it on the stack
                            frame.setLocal((int) operands[0], this.stack.peek());
                            break;
                        }
                    case GLOBAL_GET:
                        {
                            var val = instance.getGlobal((int) operands[0]);
                            this.stack.push(val);
                            break;
                        }
                    case GLOBAL_SET:
                        {
                            var id = (int) operands[0];
                            var global = instance.getGlobalInitalizers()[id];
                            if (global.getMutabilityType() == MutabilityType.Const)
                                throw new RuntimeException(
                                        "Can't call GLOBAL_SET on immutable global");
                            var val = this.stack.pop();
                            instance.setGlobal(id, val);
                            break;
                        }
                        // TODO signed and unsigned are the same right now
                    case I32_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI64(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case F32_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getF32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case F64_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getF64(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I32_LOAD8_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI8(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD8_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI8(ptr);
                            // TODO a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD8_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI8U(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD8_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI8U(ptr);
                            // TODO a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD16_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI16(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD16_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI16(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD16_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getU16(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD16_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getU16(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I64_LOAD32_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getI32(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I64_LOAD32_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().getU32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I32_STORE:
                        {
                            var value = this.stack.pop().asInt();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putI32(ptr, value);
                            break;
                        }
                    case I32_STORE16:
                    case I64_STORE16:
                        {
                            var value = this.stack.pop().asShort();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putShort(ptr, value);
                            break;
                        }
                    case I64_STORE:
                        {
                            var value = this.stack.pop().asLong();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putI64(ptr, value);
                            break;
                        }
                    case F32_STORE:
                        {
                            var value = this.stack.pop().asFloat();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putF32(ptr, value);
                            break;
                        }
                    case F64_STORE:
                        {
                            var value = this.stack.pop().asDouble();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putF64(ptr, value);
                            break;
                        }
                    case MEMORY_GROW:
                        {
                            var size = stack.pop().asInt();
                            var nPages = instance.getMemory().grow(size);
                            stack.push(Value.i32(nPages));
                            break;
                        }
                    case I32_STORE8:
                    case I64_STORE8:
                        {
                            var value = this.stack.pop().asByte();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putByte(ptr, value);
                            break;
                        }
                    case I64_STORE32:
                        {
                            var value = this.stack.pop().asInt();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().putI32(ptr, value);
                            break;
                        }
                    case MEMORY_SIZE:
                        {
                            var sz = instance.getMemory().getSize();
                            this.stack.push(Value.i32(sz));
                            break;
                        }
                        // TODO 32bit and 64 bit operations are the same for now
                    case I32_CONST:
                        {
                            this.stack.push(Value.i32(operands[0]));
                            break;
                        }
                    case I64_CONST:
                        {
                            this.stack.push(Value.i64(operands[0]));
                            break;
                        }
                    case F32_CONST:
                        {
                            this.stack.push(Value.f32(operands[0]));
                            break;
                        }
                    case F64_CONST:
                        {
                            this.stack.push(Value.f64(operands[0]));
                            break;
                        }
                    case I32_EQ:
                        {
                            var a = stack.pop().asInt();
                            var b = stack.pop().asInt();
                            this.stack.push(a == b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_EQ:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(a == b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_NE:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(a == b ? Value.FALSE : Value.TRUE);
                            break;
                        }
                    case I64_NE:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(a == b ? Value.FALSE : Value.TRUE);
                            break;
                        }
                    case I32_EQZ:
                        {
                            var a = this.stack.pop().asInt();
                            this.stack.push(a == 0 ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_EQZ:
                        {
                            var a = this.stack.pop().asLong();
                            this.stack.push(a == 0L ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_LT_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            this.stack.push(a < b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_LT_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(a < b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_LT_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(a < b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_LT_U:
                        {
                            var b = this.stack.pop().asULong();
                            var a = this.stack.pop().asULong();
                            this.stack.push(a.compareTo(b) < 0 ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_GT_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            this.stack.push(a > b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_GT_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(a > b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_GT_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(a > b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_GT_U:
                        {
                            var b = this.stack.pop().asULong();
                            var a = this.stack.pop().asULong();
                            this.stack.push(a.compareTo(b) > 0 ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_GE_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            this.stack.push(a >= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_GE_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(a >= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_GE_U:
                        {
                            var b = this.stack.pop().asULong();
                            var a = this.stack.pop().asULong();
                            this.stack.push(a.compareTo(b) >= 0 ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_GE_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(a >= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_LE_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            this.stack.push(a <= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_LE_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(a <= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_LE_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(a <= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I64_LE_U:
                        {
                            var b = this.stack.pop().asULong();
                            var a = this.stack.pop().asULong();
                            this.stack.push(a.compareTo(b) <= 0 ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F32_EQ:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(a == b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F64_EQ:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(a == b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case I32_CLZ:
                        {
                            var tos = this.stack.pop().asInt();
                            var count = Integer.numberOfLeadingZeros(tos);
                            this.stack.push(Value.i32(count));
                            break;
                        }
                    case I32_CTZ:
                        {
                            var tos = this.stack.pop().asInt();
                            var count = Integer.numberOfTrailingZeros(tos);
                            this.stack.push(Value.i32(count));
                            break;
                        }
                    case I32_POPCNT:
                        {
                            var tos = this.stack.pop().asInt();
                            var count = Integer.bitCount(tos);
                            this.stack.push(Value.i32(count));
                            break;
                        }
                    case I32_ADD:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a + b));
                            break;
                        }
                    case I64_ADD:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a + b));
                            break;
                        }
                    case I32_SUB:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(b - a));
                            break;
                        }
                    case I64_SUB:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(b - a));
                            break;
                        }
                    case I32_MUL:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a * b));
                            break;
                        }
                    case I64_MUL:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a * b));
                            break;
                        }
                    case I32_DIV_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            if (a == Integer.MIN_VALUE && b == -1) {
                                throw new WASMRuntimeException("integer overflow");
                            }
                            this.stack.push(Value.i32(a / b));
                            break;
                        }
                    case I32_DIV_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(Value.i32(a / b));
                            break;
                        }
                    case I64_DIV_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            if (a == Long.MIN_VALUE && b == -1L) {
                                throw new WASMRuntimeException("integer overflow");
                            }
                            this.stack.push(Value.i64(a / b));
                            break;
                        }
                    case I64_DIV_U:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(Value.i64(Long.divideUnsigned(a, b)));
                            break;
                        }
                    case I32_REM_S:
                        {
                            var b = this.stack.pop().asInt();
                            var a = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a % b));
                            break;
                        }
                    case I32_REM_U:
                        {
                            var b = this.stack.pop().asUInt();
                            var a = this.stack.pop().asUInt();
                            this.stack.push(Value.i32(a % b));
                            break;
                        }
                    case I64_AND:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a & b));
                            break;
                        }
                    case I64_OR:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a | b));
                            break;
                        }
                    case I64_XOR:
                        {
                            var a = this.stack.pop().asLong();
                            var b = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a ^ b));
                            break;
                        }
                    case I64_SHL:
                        {
                            var c = this.stack.pop().asLong();
                            var v = this.stack.pop().asLong();
                            this.stack.push(Value.i64(v << c));
                            break;
                        }
                    case I64_SHR_S:
                        {
                            var c = this.stack.pop().asLong();
                            var v = this.stack.pop().asLong();
                            this.stack.push(Value.i64(v >> c));
                            break;
                        }
                    case I64_SHR_U:
                        {
                            var c = this.stack.pop().asLong();
                            var v = this.stack.pop().asLong();
                            this.stack.push(Value.i64(v >>> c));
                            break;
                        }
                    case I64_REM_S:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(Value.i64(a % b));
                            break;
                        }
                    case I64_REM_U:
                        {
                            var b = this.stack.pop().asLong();
                            var a = this.stack.pop().asLong();
                            this.stack.push(Value.i64(Long.remainderUnsigned(a, b)));
                            break;
                        }
                    case I64_ROTL:
                        {
                            var c = this.stack.pop().asLong();
                            var v = this.stack.pop().asLong();
                            var z = (v << c) | (v >>> (64 - c));
                            this.stack.push(Value.i64(z));
                            break;
                        }
                    case I64_ROTR:
                        {
                            var c = this.stack.pop().asLong();
                            var v = this.stack.pop().asLong();
                            var z = (v >>> c) | (v << (64 - c));
                            this.stack.push(Value.i64(z));
                            break;
                        }
                    case I64_CLZ:
                        {
                            var tos = this.stack.pop();
                            var count = Long.numberOfLeadingZeros(tos.asLong());
                            this.stack.push(Value.i64(count));
                            break;
                        }
                    case I64_CTZ:
                        {
                            var tos = this.stack.pop();
                            var count = Long.numberOfTrailingZeros(tos.asLong());
                            this.stack.push(Value.i64(count));
                            break;
                        }
                    case I64_POPCNT:
                        {
                            var tos = this.stack.pop().asLong();
                            var count = Long.bitCount(tos);
                            this.stack.push(Value.i64(count));
                            break;
                        }
                    case F32_NEG:
                        {
                            var tos = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(-1.0f * tos));
                            break;
                        }
                    case F64_NEG:
                        {
                            var tos = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(-1.0d * tos));
                            break;
                        }
                    case CALL:
                        {
                            var funcId = (int) operands[0];
                            var typeId = instance.getFunctionTypes()[funcId];
                            var type = instance.getTypes()[typeId];
                            // given a list of param types, let's pop those params off the stack
                            // and pass as args to the function call
                            var args = extractArgsForParams(type.getParams());
                            call(funcId, args, false);
                            break;
                        }
                    case I32_AND:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a & b));
                            break;
                        }
                    case I32_OR:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a | b));
                            break;
                        }
                    case I32_XOR:
                        {
                            var a = this.stack.pop().asInt();
                            var b = this.stack.pop().asInt();
                            this.stack.push(Value.i32(a ^ b));
                            break;
                        }
                    case I32_SHL:
                        {
                            var c = this.stack.pop().asInt();
                            var v = this.stack.pop().asInt();
                            this.stack.push(Value.i32(v << c));
                            break;
                        }
                    case I32_SHR_S:
                        {
                            var c = this.stack.pop().asInt();
                            var v = this.stack.pop().asInt();
                            this.stack.push(Value.i32(v >> c));
                            break;
                        }
                    case I32_SHR_U:
                        {
                            var c = this.stack.pop().asInt();
                            var v = this.stack.pop().asInt();
                            this.stack.push(Value.i32(v >>> c));
                            break;
                        }
                    case I32_ROTL:
                        {
                            var c = this.stack.pop().asInt();
                            var v = this.stack.pop().asInt();
                            var z = (v << c) | (v >>> (32 - c));
                            this.stack.push(Value.i32(z));
                            break;
                        }
                    case I32_ROTR:
                        {
                            var c = this.stack.pop().asInt();
                            var v = this.stack.pop().asInt();
                            var z = (v >>> c) | (v << (32 - c));
                            this.stack.push(Value.i32(z));
                            break;
                        }
                    case F32_ADD:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(a + b));
                            break;
                        }
                    case F64_ADD:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(a + b));
                            break;
                        }
                    case F32_SUB:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(b - a));
                            break;
                        }
                    case F64_SUB:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(b - a));
                            break;
                        }
                    case F32_MUL:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(b * a));
                            break;
                        }
                    case F64_MUL:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(b * a));
                            break;
                        }
                    case F32_DIV:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(b / a));
                            break;
                        }
                    case F64_DIV:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(b / a));
                            break;
                        }
                    case F32_MIN:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(Math.min(a, b)));
                            break;
                        }
                    case F64_MIN:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.min(a, b)));
                            break;
                        }
                    case F32_MAX:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();
                            this.stack.push(Value.fromFloat(Math.max(a, b)));
                            break;
                        }
                    case F64_MAX:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.max(a, b)));
                            break;
                        }
                    case F32_SQRT:
                        {
                            var val = this.stack.pop().asFloat();
                            this.stack.push(
                                    Value.fromFloat(
                                            Double.valueOf(
                                                            Math.sqrt(
                                                                    Float.valueOf(val)
                                                                            .doubleValue()))
                                                    .floatValue()));
                            break;
                        }
                    case F64_SQRT:
                        {
                            var val = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.sqrt(val)));
                            break;
                        }
                    case F32_FLOOR:
                        {
                            var val = this.stack.pop().asFloat();
                            this.stack.push(
                                    Value.fromFloat(
                                            Double.valueOf(
                                                            Math.floor(
                                                                    Float.valueOf(val)
                                                                            .doubleValue()))
                                                    .floatValue()));
                            break;
                        }
                    case F64_FLOOR:
                        {
                            var val = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.floor(val)));
                            break;
                        }
                    case F32_CEIL:
                        {
                            var val = this.stack.pop().asFloat();
                            this.stack.push(
                                    Value.fromFloat(
                                            Double.valueOf(
                                                            Math.ceil(
                                                                    Float.valueOf(val)
                                                                            .doubleValue()))
                                                    .floatValue()));
                            break;
                        }
                    case F64_CEIL:
                        {
                            var val = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.ceil(val)));
                            break;
                        }
                    case F32_TRUNC:
                        {
                            var val = this.stack.pop().asFloat();
                            this.stack.push(
                                    Value.fromFloat(
                                            Double.valueOf(
                                                            (val < 0)
                                                                    ? Math.ceil(
                                                                            Float.valueOf(val)
                                                                                    .doubleValue())
                                                                    : Math.floor(
                                                                            Float.valueOf(val)
                                                                                    .doubleValue()))
                                                    .floatValue()));
                            break;
                        }
                    case F64_TRUNC:
                        {
                            var val = this.stack.pop().asDouble();
                            this.stack.push(
                                    Value.fromDouble((val < 0) ? Math.ceil(val) : Math.floor(val)));
                            break;
                        }
                    case F32_NEAREST:
                        {
                            var val = this.stack.pop().asFloat();
                            this.stack.push(
                                    Value.fromFloat(
                                            Double.valueOf(
                                                            Math.rint(
                                                                    Float.valueOf(val)
                                                                            .doubleValue()))
                                                    .floatValue()));
                            break;
                        }
                    case F64_NEAREST:
                        {
                            var val = this.stack.pop().asDouble();
                            this.stack.push(Value.fromDouble(Math.rint(val)));
                            break;
                        }
                        // For the extend_* operations, note that java
                        // automatically does this when casting from
                        // smaller to larger primitives
                    case I32_EXTEND_8_S:
                        {
                            var tos = this.stack.pop().asByte();
                            this.stack.push(Value.i32(tos));
                            break;
                        }
                    case I32_EXTEND_16_S:
                        {
                            var original = this.stack.pop().asInt() & 0xFFFF;
                            if ((original & 0x8000) != 0) original |= 0xFFFF0000;
                            this.stack.push(Value.i32(original & 0xFFFFFFFFL));
                            break;
                        }
                    case I64_EXTEND_8_S:
                        {
                            var tos = this.stack.pop().asByte();
                            this.stack.push(Value.i64(tos));
                            break;
                        }
                    case I64_EXTEND_16_S:
                        {
                            var tos = this.stack.pop().asShort();
                            this.stack.push(Value.i64(tos));
                            break;
                        }
                    case I64_EXTEND_32_S:
                        {
                            var tos = this.stack.pop().asInt();
                            this.stack.push(Value.i64(tos));
                            break;
                        }
                    case F64_CONVERT_I64_U:
                        {
                            var tos = this.stack.pop().asULong();
                            this.stack.push(Value.fromDouble(tos.doubleValue()));
                            break;
                        }
                    case F64_CONVERT_I32_U:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(
                                    Value.fromDouble(Long.valueOf(tos.asUInt()).doubleValue()));
                            break;
                        }
                    case F64_CONVERT_I32_S:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(
                                    Value.fromDouble(Long.valueOf(tos.asInt()).doubleValue()));
                            break;
                        }
                    case F64_PROMOTE_F32:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(
                                    Value.fromDouble(Float.valueOf(tos.asFloat()).doubleValue()));
                            break;
                        }
                    case F64_REINTERPRET_I64:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.f64(tos.asLong()));
                            break;
                        }
                    case I64_TRUNC_F64_S:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i64(Double.valueOf(tos.asDouble()).longValue()));
                            break;
                        }
                    case I32_WRAP_I64:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i32(tos.asInt()));
                            break;
                        }
                    case I64_EXTEND_I32_S:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i64(Integer.valueOf(tos.asInt()).longValue()));
                            break;
                        }
                    case I64_EXTEND_I32_U:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i64(tos.asUInt()));
                            break;
                        }
                    case I32_REINTERPRET_F32:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i32(tos.asInt()));
                            break;
                        }
                    case I64_REINTERPRET_F64:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.i64(tos.asLong()));
                            break;
                        }
                    case F32_REINTERPRET_I32:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.f32(tos.asInt()));
                            break;
                        }
                    case F32_COPYSIGN:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            if (a == 0xFFC00000L) { // +NaN
                                this.stack.push(Value.fromFloat(Math.copySign(b, -1)));
                            } else if (a == 0x7FC00000L) { // -NaN
                                this.stack.push(Value.fromFloat(Math.copySign(b, +1)));
                            } else {
                                this.stack.push(Value.fromFloat(Math.copySign(b, a)));
                            }
                            break;
                        }
                    case F32_ABS:
                        {
                            var val = this.stack.pop().asFloat();

                            this.stack.push(Value.fromFloat(Math.abs(Float.valueOf(val))));
                            break;
                        }
                    case F64_COPYSIGN:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            if (a == 0xFFC0000000000000L) { // +NaN
                                this.stack.push(Value.fromDouble(Math.copySign(b, -1)));
                            } else if (a == 0x7FC0000000000000L) { // -NaN
                                this.stack.push(Value.fromDouble(Math.copySign(b, +1)));
                            } else {
                                this.stack.push(Value.fromDouble(Math.copySign(b, a)));
                            }
                            break;
                        }
                    case F64_ABS:
                        {
                            var val = this.stack.pop().asDouble();

                            this.stack.push(Value.fromDouble(Math.abs(Double.valueOf(val))));
                            break;
                        }
                    case F32_NE:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            this.stack.push(a == b ? Value.FALSE : Value.TRUE);
                            break;
                        }
                    case F64_NE:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            this.stack.push(a == b ? Value.FALSE : Value.TRUE);
                            break;
                        }
                    case F32_LT:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            this.stack.push(a > b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F64_LT:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            this.stack.push(a > b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F32_LE:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            this.stack.push(a >= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F64_LE:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            this.stack.push(a >= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F32_GE:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            this.stack.push(a <= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F64_GE:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            this.stack.push(a <= b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F32_GT:
                        {
                            var a = this.stack.pop().asFloat();
                            var b = this.stack.pop().asFloat();

                            this.stack.push(a < b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F64_GT:
                        {
                            var a = this.stack.pop().asDouble();
                            var b = this.stack.pop().asDouble();

                            this.stack.push(a < b ? Value.TRUE : Value.FALSE);
                            break;
                        }
                    case F32_DEMOTE_F64:
                        {
                            var val = this.stack.pop().asDouble();

                            this.stack.push(Value.fromFloat(Double.valueOf(val).floatValue()));
                            break;
                        }
                    case F32_CONVERT_I32_S:
                        {
                            var val = this.stack.pop().asInt();

                            this.stack.push(Value.fromFloat(Integer.valueOf(val).floatValue()));
                            break;
                        }
                    case I32_TRUNC_F32_S:
                        {
                            var val = this.stack.pop().asFloat();

                            this.stack.push(Value.i32(Float.valueOf(val).intValue()));
                            break;
                        }
                    case F32_CONVERT_I32_U:
                        {
                            var val = this.stack.pop().asUInt();

                            this.stack.push(Value.fromFloat(Long.valueOf(val).floatValue()));
                            break;
                        }
                    case I32_TRUNC_F32_U:
                        {
                            var val = this.stack.pop().asFloat();

                            this.stack.push(Value.i32(Float.valueOf(val).longValue()));
                            break;
                        }
                    case F64_CONVERT_I64_S:
                        {
                            var val = this.stack.pop().asLong();

                            this.stack.push(Value.fromDouble(Long.valueOf(val).doubleValue()));
                            break;
                        }
                    case I64_TRUNC_F64_U:
                        {
                            var val = this.stack.pop().asDouble();

                            this.stack.push(Value.i64(Double.valueOf(val).longValue()));
                            break;
                        }
                    case I32_TRUNC_F64_S:
                        {
                            var val = this.stack.pop().asDouble();

                            this.stack.push(Value.i32(Double.valueOf(val).intValue()));
                            break;
                        }
                    case I32_TRUNC_F64_U:
                        {
                            var val = this.stack.pop().asDouble();

                            this.stack.push(Value.i32(Double.valueOf(val).longValue()));
                            break;
                        }
                    case I64_TRUNC_F32_S:
                        {
                            var val = this.stack.pop().asFloat();

                            this.stack.push(Value.i64(Float.valueOf(val).longValue()));
                            break;
                        }
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        }
    }

    public void printStackTrace() {
        System.out.println("Trapped. Stacktrace:");
        for (var f : callStack) {
            System.out.println(f);
        }
    }

    Value[] extractArgsForParams(ValueType[] params) {
        if (params == null) return new Value[] {};
        var args = new Value[params.length];
        for (var i = params.length; i > 0; i--) {
            var p = this.stack.pop();
            var t = params[i - 1];
            if (p.getType() != t) {
                throw new RuntimeException("Type error when extracting args.");
            }
            args[i - 1] = p;
        }
        return args;
    }
}
