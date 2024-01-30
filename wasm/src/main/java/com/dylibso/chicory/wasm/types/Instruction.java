package com.dylibso.chicory.wasm.types;

import java.util.Arrays;

public class Instruction {
    private int address;
    private OpCode opcode;
    private long[] operands;
    private CodeBlock block;

    // metadata fields
    private Integer labelTrue;
    private Integer labelFalse;
    private int[] labelTable;
    private Integer depth;
    private Instruction scope;

    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands;
    }

    public OpCode opcode() {
        return opcode;
    }

    public long[] operands() {
        return operands;
    }

    public void setCodeBlock(CodeBlock block) {
        this.block = block;
    }

    public CodeBlock codeBlock() {
        return block;
    }

    public String toString() {
        var result = String.format("0x%08X", address) + ": ";
        if (operands.length > 0) {
            return result + opcode + " " + Arrays.toString(operands);
        }
        return result + opcode.toString();
    }

    public int address() {
        return address;
    }

    public Integer labelTrue() {
        return labelTrue;
    }

    public void setLabelTrue(Integer labelTrue) {
        this.labelTrue = labelTrue;
    }

    public Integer labelFalse() {
        return labelFalse;
    }

    public void setLabelFalse(Integer labelFalse) {
        this.labelFalse = labelFalse;
    }

    public int[] labelTable() {
        return labelTable;
    }

    public void setLabelTable(int[] labelTable) {
        this.labelTable = labelTable;
    }

    public Integer depth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Instruction scope() {
        return scope;
    }

    public void setScope(Instruction scope) {
        this.scope = scope;
    }
}
