package com.dylibso.chicory.experimental.aot;

import java.util.Arrays;
import java.util.stream.LongStream;

final class AotInstruction {
    public static final long[] EMPTY = new long[0];

    private final AotOpCode opcode;
    private final long[] operands;

    public AotInstruction(AotOpCode opcode) {
        this(opcode, EMPTY);
    }

    public AotInstruction(AotOpCode opcode, long operand) {
        this(opcode, new long[] {operand});
    }

    public AotInstruction(AotOpCode opcode, long[] operands) {
        this.opcode = opcode;
        this.operands = operands;
    }

    public AotOpCode opcode() {
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
