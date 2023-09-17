package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.List;

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
    private OpCode scope;

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

    public Integer getLabelTrue() {
        return labelTrue;
    }

    public void setLabelTrue(Integer labelTrue) {
        this.labelTrue = labelTrue;
    }

    public Integer getLabelFalse() {
        return labelFalse;
    }

    public void setLabelFalse(Integer labelFalse) {
        this.labelFalse = labelFalse;
    }

    public int[] getLabelTable() {
        return labelTable;
    }

    public void setLabelTable(int[] labelTable) {
        this.labelTable = labelTable;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public OpCode getScope() {
        return scope;
    }

    public void setScope(OpCode scope) {
        this.scope = scope;
    }
}
