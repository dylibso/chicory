package com.dylibso.chicory.compiler.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.LongStream;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.InstructionAdapter;

final class CompilerInstruction {

    interface Emitter {
        // We should modify this method signature as needed so we can supply impls the data they
        // need to emit the instruction.
        void emmit(Context ctx, InstructionAdapter asm, Map<Long, Label> labels);
    }

    public static final long[] EMPTY = new long[0];

    private final CompilerOpCode opcode;
    private final long[] operands;
    private final Emitter emitter;

    public CompilerInstruction(CompilerOpCode opcode) {
        this(opcode, EMPTY);
    }

    public CompilerInstruction(CompilerOpCode opcode, long... operands) {
        this.opcode = opcode;
        this.operands = operands;
        this.emitter = null;
    }

    public CompilerInstruction(long[] operands, Emitter emmitter) {
        this.opcode = CompilerOpCode.EMITTER;
        this.operands = operands;
        this.emitter = Objects.requireNonNull(emmitter);
    }

    public CompilerOpCode opcode() {
        return opcode;
    }

    public Emitter emitter() {
        return emitter;
    }

    public LongStream operands() {
        return Arrays.stream(operands);
    }

    public int operandCount() {
        return operands.length;
    }

    public long operand(int index) {
        return operands[index];
    }

    @Override
    public String toString() {
        if (operands.length == 0) {
            return opcode.toString();
        }
        if (operands.length == 1) {
            return opcode + " " + operands[0];
        }
        return opcode + " " + Arrays.toString(operands);
    }

    public long[] labelTargets() {
        switch (opcode) {
            case GOTO:
            case IFEQ:
            case IFNE:
            case EMITTER:
            case SWITCH:
                return operands;
            default:
                return EMPTY;
        }
    }
}
