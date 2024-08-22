package com.dylibso.chicory.wasm.types;

import java.util.Arrays;

public class Instruction {
    private final int address;
    private final OpCode opcode;
    private final long[] operands;

    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands;
    }

    public int address() {
        return address;
    }

    public OpCode opcode() {
        return opcode;
    }

    public long[] operands() {
        return operands;
    }

    @Override
    public String toString() {
        var result = String.format("0x%08X", address) + ": ";
        if (operands.length > 0) {
            return result + opcode + " " + Arrays.toString(operands);
        }
        return result + opcode.toString();
    }
}
