package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.Table.UNINITIALIZED;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import java.util.Stack;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
public class Machine {

    private static final System.Logger LOGGER = System.getLogger(Machine.class.getName());

    public static final double TWO_POW_63_D = 0x1.0p63; /* 2^63 */

    public static final float TWO_POW_64_PLUS_1_F = 1.8446743E19F; /* 2^64 + 1*/

    private final MStack stack;

    private final Stack<StackFrame> callStack;

    private final Instance instance;

    public Machine(Instance instance) {
        this.instance = instance;
        this.stack = new MStack();
        this.callStack = new Stack<>();
    }

    public Value[] call(int funcId, Value[] args, boolean popResults) throws ChicoryException {
        var func = instance.getFunction(funcId);
        if (func != null) {
            this.callStack.push(new StackFrame(instance, funcId, 0, args, func.getLocals()));
            eval(func.getInstructions());
        } else {
            this.callStack.push(new StackFrame(instance, funcId, 0, args, List.of()));
            var imprt = instance.getImports().getIndex()[funcId];
            if (imprt == null) {
                throw new ChicoryException("Missing host import, number: " + funcId);
            }

            switch (imprt.getType()) {
                case FUNCTION:
                    var hostFunc = ((HostFunction) imprt).getHandle();
                    var results = hostFunc.apply(this.instance.getMemory(), args);
                    // a host function can return null or an array of ints
                    // which we will push onto the stack
                    if (results != null) {
                        for (var result : results) {
                            this.stack.push(result);
                        }
                    }
                    break;
                case GLOBAL:
                    this.stack.push(((HostGlobal) imprt).getValue());
                    break;
                default:
                    throw new ChicoryException("Not implemented");
            }
        }

        if (!this.callStack.isEmpty()) {
            this.callStack.pop();
        }

        if (!popResults) {
            return null;
        }

        var typeId = instance.getFunctionType(funcId);
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
                var instruction = code.get(frame.pc);
                frame.pc++;
                if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                    LOGGER.log(
                            System.Logger.Level.DEBUG,
                            "func={0}@{1}:{2} stack={3}",
                            frame.funcId,
                            frame.pc,
                            instruction,
                            this.stack);
                }
                var opcode = instruction.getOpcode();
                var operands = instruction.getOperands();
                switch (opcode) {
                    case UNREACHABLE:
                        throw new TrapException("Trapped on unreachable instruction", callStack);
                    case NOP:
                        break;
                    case LOOP:
                    case BLOCK:
                        {
                            frame.blockDepth++;

                            frame.isControlFrame = true;
                            frame.stackSizeBeforeBlock =
                                    Math.max(this.stack.size(), frame.stackSizeBeforeBlock);
                            var typeId = (int) operands[0];

                            // https://www.w3.org/TR/wasm-core-2/binary/instructions.html#binary-blocktype
                            if (typeId == 0x40) { // epsilon
                                frame.numberOfValuesToReturn =
                                        Math.max(frame.numberOfValuesToReturn, 0);
                            } else if (ValueType.byId(typeId)
                                    != null) { // shortcut to straight value type
                                frame.numberOfValuesToReturn =
                                        Math.max(frame.numberOfValuesToReturn, 1);
                            } else { // look it up
                                var funcType = instance.getTypes()[typeId];
                                frame.numberOfValuesToReturn =
                                        Math.max(
                                                frame.numberOfValuesToReturn,
                                                funcType.getReturns().length);
                            }

                            break;
                        }
                    case IF:
                        {
                            frame.blockDepth++;
                            frame.isControlFrame = false;

                            var predValue = this.stack.pop();
                            var pred = predValue.asInt();
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
                            prepareControlTransfer(frame, false);

                            frame.pc = instruction.getLabelTrue();
                            break;
                        }
                    case BR_IF:
                        {
                            var predValue = prepareControlTransfer(frame, true);
                            var pred = predValue.asInt();

                            if (pred == 0) {
                                frame.pc = instruction.getLabelFalse();
                            } else {
                                frame.branchConditionValue = predValue;
                                frame.pc = instruction.getLabelTrue();
                            }
                            break;
                        }
                    case BR_TABLE:
                        {
                            var predValue = prepareControlTransfer(frame, true);
                            var pred = predValue.asInt();

                            if (pred < 0 || pred >= instruction.getLabelTable().length - 1) {
                                // choose default
                                frame.pc =
                                        instruction
                                                .getLabelTable()[
                                                instruction.getLabelTable().length - 1];
                            } else {
                                frame.branchConditionValue = predValue;
                                frame.pc = instruction.getLabelTable()[pred];
                            }

                            break;
                        }
                    case RETURN:
                        shouldReturn = true;
                        break;
                    case CALL_INDIRECT:
                        {
                            var tableIdx = (int) operands[1];
                            var table = instance.getTable(tableIdx);
                            if (table == null) { // imported table
                                table = instance.getImports().getTables()[tableIdx].getTable();
                            }
                            var typeId = (int) operands[0];
                            var type = instance.getTypes()[typeId];
                            int funcTableIdx = this.stack.pop().asInt();
                            int funcId = table.getRef(funcTableIdx).asFuncRef();
                            if (funcId == UNINITIALIZED) {
                                throw new ChicoryException("uninitialized element");
                            } else if (funcId == REF_NULL_VALUE) {
                                throw new ChicoryException("undefined element");
                            }
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
                            if (frame.doControlTransfer && frame.isControlFrame) {
                                doControlTransfer(frame);
                            }

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
                            int idx = (int) operands[0];
                            var val = instance.getGlobal(idx);
                            if (val == null) {
                                val = instance.getImports().getGlobals()[idx].getValue();
                            }
                            this.stack.push(val);
                            break;
                        }
                    case GLOBAL_SET:
                        {
                            var id = (int) operands[0];
                            var mutabilityType =
                                    (instance.getGlobalInitalizer(id) == null)
                                            ? instance.getImports()
                                                    .getGlobals()[id]
                                                    .getMutabilityType()
                                            : instance.getGlobalInitalizer(id);
                            if (mutabilityType == MutabilityType.Const) {
                                throw new RuntimeException(
                                        "Can't call GLOBAL_SET on immutable global");
                            }
                            var val = this.stack.pop();
                            instance.setGlobal(id, val);
                            break;
                        }
                    case TABLE_GET:
                        {
                            var idx = (int) operands[0];
                            var table = instance.getTable(idx);
                            if (table == null) {
                                table = instance.getImports().getTables()[idx].getTable();
                            }
                            var i = this.stack.pop().asInt();
                            if (i < 0
                                    || (table.getLimitMax() != 0 && i >= table.getLimitMax())
                                    || i >= table.getLimitMin()) {
                                throw new WASMRuntimeException("out of bounds table access");
                            }
                            var ref = table.getRef(i);
                            this.stack.push(table.getRef(i));
                            break;
                        }
                    case TABLE_SET:
                        {
                            var idx = (int) operands[0];
                            var table = instance.getTable(idx);
                            if (table == null) {
                                table = instance.getImports().getTables()[idx].getTable();
                            }
                            var value = this.stack.pop().asExtRef();
                            var i = this.stack.pop().asInt();
                            table.setRef(i, value);
                            break;
                        }
                        // TODO signed and unsigned are the same right now
                    case I32_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI64(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case F32_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readF32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case F64_LOAD:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readF64(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I32_LOAD8_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI8(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD8_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI8(ptr);
                            // TODO a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD8_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readU8(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD8_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readU8(ptr);
                            // TODO a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD16_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI16(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD16_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI16(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I32_LOAD16_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readU16(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I64_LOAD16_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readU16(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I64_LOAD32_S:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readI32(ptr);
                            // TODO this is a bit hacky
                            this.stack.push(Value.i64(val.asInt()));
                            break;
                        }
                    case I64_LOAD32_U:
                        {
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            var val = instance.getMemory().readU32(ptr);
                            this.stack.push(val);
                            break;
                        }
                    case I32_STORE:
                        {
                            var value = this.stack.pop().asInt();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeI32(ptr, value);
                            break;
                        }
                    case I32_STORE16:
                    case I64_STORE16:
                        {
                            var value = this.stack.pop().asShort();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeShort(ptr, value);
                            break;
                        }
                    case I64_STORE:
                        {
                            var value = this.stack.pop().asLong();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeLong(ptr, value);
                            break;
                        }
                    case F32_STORE:
                        {
                            var value = this.stack.pop().asFloat();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeF32(ptr, value);
                            break;
                        }
                    case F64_STORE:
                        {
                            var value = this.stack.pop().asDouble();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeF64(ptr, value);
                            break;
                        }
                    case MEMORY_GROW:
                        {
                            var size = stack.pop().asInt();
                            var nPages = instance.getMemory().grow(size);
                            stack.push(Value.i32(nPages));
                            break;
                        }
                    case MEMORY_FILL:
                        {
                            var memidx = (int) operands[0];
                            if (memidx != 0) {
                                throw new WASMRuntimeException(
                                        "We don't support multiple memories just yet");
                            }
                            var size = stack.pop().asInt();
                            var val = stack.pop().asByte();
                            var offset = stack.pop().asInt();
                            var end = (size + offset);
                            instance.getMemory().fill(val, offset, end);
                            break;
                        }
                    case I32_STORE8:
                    case I64_STORE8:
                        {
                            var value = this.stack.pop().asByte();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeByte(ptr, value);
                            break;
                        }
                    case I64_STORE32:
                        {
                            var value = this.stack.pop().asLong();
                            var ptr = (int) (operands[1] + this.stack.pop().asInt());
                            instance.getMemory().writeI32(ptr, (int) value);
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

                            float result;
                            if (Float.isNaN(tos)) {
                                result =
                                        Float.intBitsToFloat(
                                                Float.floatToRawIntBits(tos) ^ 0x80000000);
                            } else {
                                result = -1.0f * tos;
                            }

                            this.stack.push(Value.fromFloat(result));
                            break;
                        }
                    case F64_NEG:
                        {
                            var tos = this.stack.pop().asDouble();

                            double result;
                            if (Double.isNaN(tos)) {
                                result =
                                        Double.longBitsToDouble(
                                                Double.doubleToRawLongBits(tos)
                                                        ^ 0x8000000000000000L);
                            } else {
                                result = -1.0d * tos;
                            }

                            this.stack.push(Value.fromDouble(result));
                            break;
                        }
                    case CALL:
                        {
                            var funcId = (int) operands[0];
                            var typeId = instance.getFunctionType(funcId);
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
                            this.stack.push(Value.fromFloat((float) Math.sqrt(val)));
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
                            this.stack.push(Value.fromFloat((float) Math.floor(val)));
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
                            this.stack.push(Value.fromFloat((float) Math.ceil(val)));
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
                                            (float)
                                                    ((val < 0)
                                                            ? Math.ceil(val)
                                                            : Math.floor(val))));
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
                            this.stack.push(Value.fromFloat((float) Math.rint(val)));
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
                            long tos = this.stack.pop().asUInt();
                            this.stack.push(Value.f64(Double.doubleToRawLongBits(tos)));
                            break;
                        }
                    case F64_CONVERT_I32_S:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.fromDouble(tos.asInt()));
                            break;
                        }
                    case F64_PROMOTE_F32:
                        {
                            var tos = this.stack.pop();
                            this.stack.push(Value.fromDouble(tos.asFloat()));
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
                            double tos = this.stack.pop().asDouble();

                            if (Double.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            long tosL = (long) tos;
                            if (tos == (double) Long.MIN_VALUE) {
                                tosL = Long.MIN_VALUE;
                            } else if (tosL == Long.MIN_VALUE || tosL == Long.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            this.stack.push(Value.i64(tosL));
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
                            this.stack.push(Value.i64(tos.asInt()));
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

                            this.stack.push(Value.fromFloat(Math.abs(val)));
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

                            this.stack.push(Value.fromDouble(Math.abs(val)));
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

                            this.stack.push(Value.fromFloat((float) val));
                            break;
                        }
                    case F32_CONVERT_I32_S:
                        {
                            var tos = this.stack.pop().asInt();
                            this.stack.push(Value.fromFloat((float) tos));
                            break;
                        }
                    case I32_TRUNC_F32_S:
                        {
                            float tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            if (tos < Integer.MIN_VALUE || tos >= Integer.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            this.stack.push(Value.i32((long) tos));
                            break;
                        }

                    case I32_TRUNC_SAT_F32_S:
                        {
                            var tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                tos = 0;
                            } else if (tos < Integer.MIN_VALUE) {
                                tos = Integer.MIN_VALUE;
                            } else if (tos > Integer.MAX_VALUE) {
                                tos = Integer.MAX_VALUE;
                            }

                            this.stack.push(Value.i32((int) tos));
                            break;
                        }
                    case I32_TRUNC_SAT_F32_U:
                        {
                            var tos = this.stack.pop().asFloat();

                            long tosL;
                            if (Float.isNaN(tos) || tos < 0) {
                                tosL = 0L;
                            } else if (tos >= 0xFFFFFFFFL) {
                                tosL = 0xFFFFFFFFL;
                            } else {
                                tosL = (long) tos;
                            }

                            this.stack.push(Value.i32(tosL));
                            break;
                        }

                    case I32_TRUNC_SAT_F64_S:
                        {
                            var tos = this.stack.pop().asDouble();

                            if (Double.isNaN(tos)) {
                                tos = 0;
                            } else if (tos <= Integer.MIN_VALUE) {
                                tos = Integer.MIN_VALUE;
                            } else if (tos >= Integer.MAX_VALUE) {
                                tos = Integer.MAX_VALUE;
                            }

                            this.stack.push(Value.i32((int) tos));
                            break;
                        }
                    case I32_TRUNC_SAT_F64_U:
                        {
                            double tos = Double.longBitsToDouble(this.stack.pop().asLong());

                            long tosL;
                            if (Double.isNaN(tos) || tos < 0) {
                                tosL = 0;
                            } else if (tos > 0xFFFFFFFFL) {
                                tosL = 0xFFFFFFFFL;
                            } else {
                                tosL = (long) tos;
                            }
                            this.stack.push(Value.i32(tosL));
                            break;
                        }
                    case F32_CONVERT_I32_U:
                        {
                            var tos = this.stack.pop().asUInt();

                            this.stack.push(Value.fromFloat((float) tos));
                            break;
                        }
                    case I32_TRUNC_F32_U:
                        {
                            var tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            long tosL = (long) tos;
                            if (tosL < 0 || tosL >= 0xFFFFFFFFL) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            this.stack.push(Value.i32(tosL));
                            break;
                        }
                    case F32_CONVERT_I64_S:
                        {
                            var tos = this.stack.pop().asLong();

                            this.stack.push(Value.fromFloat((float) tos));
                            break;
                        }
                    case F32_CONVERT_I64_U:
                        {
                            var tos = this.stack.pop().asULong();
                            float tosF;
                            if (tos.floatValue() < 0) {
                                /*
                                (the BigInteger is large, sign bit is set), tos.longValue() gets the lower 64 bits of the BigInteger (as a signed long),
                                and 0x1.0p63 (which is 2^63 in floating-point notation) is added to adjust the float value back to the unsigned range.
                                 */
                                tosF = (float) (tos.longValue() + TWO_POW_63_D);
                            } else {
                                tosF = tos.floatValue();
                            }
                            this.stack.push(Value.f32(Float.floatToIntBits(tosF)));
                            break;
                        }
                    case F64_CONVERT_I64_S:
                        {
                            var tos = this.stack.pop().asLong();

                            this.stack.push(Value.fromDouble((double) tos));
                            break;
                        }
                    case I64_TRUNC_F32_U:
                        {
                            var tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            if (tos >= 2 * (float) Long.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            long tosL;
                            if (tos < Long.MAX_VALUE) {
                                tosL = (long) tos;
                                if (tosL < 0) {
                                    throw new WASMRuntimeException("integer overflow");
                                }
                            } else {
                                // This works for getting the unsigned value because binary addition
                                // yields the correct interpretation in both unsigned &
                                // 2's-complement
                                // no matter which the operands are considered to be.
                                tosL = Long.MAX_VALUE + (long) (tos - (float) Long.MAX_VALUE) + 1;
                                if (tosL >= 0) {
                                    // Java's comparison operators assume signed integers. In the
                                    // case
                                    // that we're in the range of unsigned values where the sign bit
                                    // is set, Java considers these values to be negative so we have
                                    // to check for >= 0 to detect overflow.
                                    throw new WASMRuntimeException("integer overflow");
                                }
                            }

                            this.stack.push(Value.i64(tosL));
                            break;
                        }
                    case I64_TRUNC_F64_U:
                        {
                            var tos = this.stack.pop().asDouble();

                            if (Double.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            if (tos >= 2 * (double) Long.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            long tosL;
                            if (tos < Long.MAX_VALUE) {
                                tosL = (long) tos;
                                if (tosL < 0) {
                                    throw new WASMRuntimeException("integer overflow");
                                }
                            } else {
                                // See I64_TRUNC_F32_U for notes on implementation. This is
                                // the double-based equivalent of that.
                                tosL = Long.MAX_VALUE + (long) (tos - (double) Long.MAX_VALUE) + 1;
                                if (tosL >= 0) {
                                    throw new WASMRuntimeException("integer overflow");
                                }
                            }

                            this.stack.push(Value.i64(tosL));
                            break;
                        }

                    case I64_TRUNC_SAT_F32_S:
                        {
                            var tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                tos = 0;
                            } else if (tos <= Long.MIN_VALUE) {
                                tos = Long.MIN_VALUE;
                            } else if (tos >= Long.MAX_VALUE) {
                                tos = Long.MAX_VALUE;
                            }

                            this.stack.push(Value.i64((long) tos));
                            break;
                        }
                    case I64_TRUNC_SAT_F32_U:
                        {
                            var tos = this.stack.pop().asFloat();

                            long tosL;
                            if (Float.isNaN(tos) || tos < 0) {
                                tosL = 0L;
                            } else if (tos > Math.pow(2, 64) - 1) {
                                tosL = 0xFFFFFFFFFFFFFFFFL;
                            } else {
                                if (tos < Long.MAX_VALUE) {
                                    tosL = (long) tos;
                                } else {
                                    // See I64_TRUNC_F32_U for notes on implementation. This is
                                    // the double-based equivalent of that.
                                    tosL =
                                            Long.MAX_VALUE
                                                    + (long) (tos - (double) Long.MAX_VALUE)
                                                    + 1;
                                    if (tosL >= 0) {
                                        throw new WASMRuntimeException("integer overflow");
                                    }
                                }
                            }

                            this.stack.push(Value.i64(tosL));
                            break;
                        }
                    case I64_TRUNC_SAT_F64_S:
                        {
                            var tos = this.stack.pop().asDouble();

                            if (Double.isNaN(tos)) {
                                tos = 0;
                            } else if (tos <= Long.MIN_VALUE) {
                                tos = Long.MIN_VALUE;
                            } else if (tos >= Long.MAX_VALUE) {
                                tos = Long.MAX_VALUE;
                            }

                            this.stack.push(Value.i64((long) tos));
                            break;
                        }

                    case I64_TRUNC_SAT_F64_U:
                        {
                            double tos = this.stack.pop().asDouble();

                            long tosL;
                            if (Double.isNaN(tos) || tos < 0) {
                                tosL = 0L;
                            } else if (tos > Math.pow(2, 64) - 1) {
                                tosL = 0xFFFFFFFFFFFFFFFFL;
                            } else {
                                if (tos < Long.MAX_VALUE) {
                                    tosL = (long) tos;
                                } else {
                                    // See I64_TRUNC_F32_U for notes on implementation. This is
                                    // the double-based equivalent of that.
                                    tosL =
                                            Long.MAX_VALUE
                                                    + (long) (tos - (double) Long.MAX_VALUE)
                                                    + 1;
                                    if (tosL >= 0) {
                                        throw new WASMRuntimeException("integer overflow");
                                    }
                                }
                            }

                            this.stack.push(Value.i64(tosL));
                            break;
                        }

                    case I32_TRUNC_F64_S:
                        {
                            var tos = this.stack.pop().asDouble();

                            if (Double.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            var tosL = (long) tos;
                            if (tosL < Integer.MIN_VALUE || tosL > Integer.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            this.stack.push(Value.i32(tosL));
                            break;
                        }
                    case I32_TRUNC_F64_U:
                        {
                            double tos = this.stack.pop().asDouble();
                            if (Double.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            var tosL = (long) tos;
                            if (tosL < 0 || tosL > 0xFFFFFFFFL) {
                                throw new WASMRuntimeException("integer overflow");
                            }
                            this.stack.push(Value.i32(tosL & 0xFFFFFFFFL));
                            break;
                        }
                    case I64_TRUNC_F32_S:
                        {
                            var tos = this.stack.pop().asFloat();

                            if (Float.isNaN(tos)) {
                                throw new WASMRuntimeException("invalid conversion to integer");
                            }

                            if (tos < Long.MIN_VALUE || tos >= Long.MAX_VALUE) {
                                throw new WASMRuntimeException("integer overflow");
                            }

                            this.stack.push(Value.i64((long) tos));
                            break;
                        }
                    case MEMORY_INIT:
                        {
                            var segmentId = (int) operands[0];
                            var memidx = (int) operands[1];
                            if (memidx != 0)
                                throw new WASMRuntimeException(
                                        "We don't support non zero index for memory: " + memidx);
                            var size = this.stack.pop().asInt();
                            var offset = this.stack.pop().asInt();
                            var destination = this.stack.pop().asInt();
                            instance.getMemory()
                                    .initPassiveSegment(segmentId, destination, offset, size);
                            break;
                        }
                    case DATA_DROP:
                        {
                            var segment = (int) operands[0];
                            instance.getMemory().drop(segment);
                            break;
                        }
                    case MEMORY_COPY:
                        {
                            var memidxSrc = (int) operands[0];
                            var memidxDst = (int) operands[1];
                            if (memidxDst != 0 && memidxSrc != 0)
                                throw new WASMRuntimeException(
                                        "We don't support non zero index for memory: "
                                                + memidxSrc
                                                + " "
                                                + memidxDst);
                            var size = this.stack.pop().asInt();
                            var offset = this.stack.pop().asInt();
                            var destination = this.stack.pop().asInt();
                            instance.getMemory().copy(destination, offset, size);
                            break;
                        }
                    case REF_IS_NULL:
                        {
                            var val = this.stack.pop();
                            this.stack.push(
                                    val.equals(Value.EXTREF_NULL) ? Value.TRUE : Value.FALSE);
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
        } catch (IndexOutOfBoundsException e) {
            throw new WASMRuntimeException("undefined element " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WASMRuntimeException("An underlying Java exception occurred", e);
        }
    }

    private Value prepareControlTransfer(StackFrame frame, boolean consume) {
        frame.doControlTransfer = true;

        var unwindStack = this.stack.getUnwindFrame();
        this.stack.resetUnwindFrame();
        Value predValue = null;
        if (consume) {
            predValue = this.stack.pop();
        }
        if (unwindStack == null) {
            this.stack.setUnwindFrame(new Stack());
        } else {
            this.stack.setUnwindFrame(unwindStack);
        }

        return predValue;
    }

    private void doControlTransfer(StackFrame frame) {
        // reset the control transfer
        frame.doControlTransfer = false;
        var unwindStack = this.stack.getUnwindFrame();
        this.stack.resetUnwindFrame();

        Value[] returns = new Value[frame.numberOfValuesToReturn];
        for (int i = 0; i < returns.length; i++) {
            if (this.stack.size() > 0) returns[i] = this.stack.pop();
        }

        // drop everything till the previous label
        if (frame.blockDepth > 0) {
            while (this.stack.size() > frame.stackSizeBeforeBlock) {
                this.stack.pop();
            }
        }

        // this is mostly empirical
        // if a branch have been taken we restore the consumed value from
        // the stack
        if (frame.branchConditionValue != null && frame.branchConditionValue.asInt() > 0) {
            this.stack.push(frame.branchConditionValue);
        }

        if (frame.blockDepth == 0) {
            while (!unwindStack.empty()) {
                this.stack.push(unwindStack.pop());
            }
        }

        for (int i = 0; i < returns.length; i++) {
            Value value = returns[returns.length - 1 - i];
            if (value != null) {
                this.stack.push(value);
            }
        }
    }

    public void printStackTrace() {
        LOGGER.log(System.Logger.Level.ERROR, "Trapped. Stacktrace:");
        for (var f : callStack) {
            LOGGER.log(System.Logger.Level.ERROR, f);
        }
    }

    Value[] extractArgsForParams(ValueType[] params) {
        if (params == null) {
            return Value.EMPTY_VALUES;
        }
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
