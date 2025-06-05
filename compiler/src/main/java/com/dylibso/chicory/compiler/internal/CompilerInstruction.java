package com.dylibso.chicory.compiler.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.LongStream;

final class CompilerInstruction {

    public static final long[] EMPTY = new long[0];

    private final CompilerOpCode opcode;
    private final long[] operands;
    private final Emitters.Emitter emitter;

    public CompilerInstruction(CompilerOpCode opcode) {
        this(opcode, EMPTY);
    }

    public CompilerInstruction(CompilerOpCode opcode, long... operands) {
        this.opcode = opcode;
        this.operands = operands;
        this.emitter = null;
    }

    public CompilerInstruction(Emitters.Emitter emitter) {
        this.opcode = CompilerOpCode.EMITTER;
        this.operands = EMPTY;
        this.emitter = Objects.requireNonNull(emitter);
    }

    public CompilerInstruction(long[] labelTargets, Emitters.Emitter emitter) {
        this.opcode = CompilerOpCode.EMITTER;
        this.operands = labelTargets;
        this.emitter = Objects.requireNonNull(emitter);
    }

    public CompilerOpCode opcode() {
        return opcode;
    }

    public Emitters.Emitter emitter() {
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
