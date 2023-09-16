package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.List;

public class Instruction {
    private int address;
    private OpCode opcode;
    private long[] operands;
    private CodeBlock block;

    // metadata fields
    public Integer labelTrue;
    public Integer labelFalse;
    public int[] labelTable;
    public Integer depth;
    public OpCode scope;

    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands;
    }

    public OpCode getOpcode() {
        return opcode;
    }

    public long[] getOperands() {
        return operands;
    }

    public void setCodeBlock(CodeBlock block) {
        this.block = block;
    }

    public CodeBlock getCodeBlock() {
        return block;
    }

    public String toString() {
        if (operands.length > 0) {
            return opcode + " " + Arrays.toString(operands);
        }
        return opcode.toString();
    }

    public int getAddress() {
        return address;
    }
}
