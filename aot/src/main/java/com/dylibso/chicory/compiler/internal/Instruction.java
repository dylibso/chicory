package com.dylibso.chicory.compiler.internal;

import java.util.Arrays;
import java.util.stream.LongStream;

final class Instruction {
    public static final long[] EMPTY = new long[0];

    private final OpCode opcode;
    private final long[] operands;

    public Instruction(OpCode opcode) {
        this(opcode, EMPTY);
    }

    public Instruction(OpCode opcode, long operand) {
        this(opcode, new long[] {operand});
    }

    public Instruction(OpCode opcode, long[] operands) {
        this.opcode = opcode;
        this.operands = operands;
    }

    public OpCode opcode() {
        return opcode;
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
            case SWITCH:
                return operands;
            default:
                return EMPTY;
        }
    }
}
