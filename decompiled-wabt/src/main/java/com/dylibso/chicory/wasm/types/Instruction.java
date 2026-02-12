package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

public class Instruction {
    public static final long[] EMPTY_OPERANDS = new long[0];

    private final int address;
    private final OpCode opcode;
    private final long[] operands;

    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands.length == 0 ? EMPTY_OPERANDS : operands.clone();
    }

    public int address() {
        return address;
    }

    public OpCode opcode() {
        return opcode;
    }

    public long[] operands() {
        return operands.clone();
    }

    public int operandCount() {
        return operands.length;
    }

    public long operand(int index) {
        return operands[index];
    }

    // this is effectively internal API used to infer some operation's
    // polymorphic type at validation time.
    public void setOperand(int index, long value) {
        operands[index] = value;
    }

    @Override
    public String toString() {
        var result = String.format("0x%08X", address) + ": ";
        if (operands.length > 0) {
            return result + opcode + " " + Arrays.toString(operands);
        }
        return result + opcode.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Instruction)) {
            return false;
        }
        Instruction that = (Instruction) o;
        return address == that.address
                && opcode == that.opcode
                && Objects.deepEquals(operands, that.operands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, opcode, Arrays.hashCode(operands));
    }
}
