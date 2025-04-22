package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a single WebAssembly instruction.
 * Contains the instruction's address (offset within the function body),
 * its {@link OpCode}, and any immediate operands it might have.
 */
public class Instruction {
    /** Constant representing an empty operands array. */
    public static final long[] EMPTY_OPERANDS = new long[0];

    private final int address;
    private final OpCode opcode;
    private final long[] operands;

    /**
     * Constructs a new Instruction.
     *
     * @param address the byte offset of this instruction within the function's code.
     * @param opcode the {@link OpCode} of the instruction.
     * @param operands an array of immediate operands for this instruction (will be cloned).
     */
    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands.length == 0 ? EMPTY_OPERANDS : operands.clone();
    }

    /**
     * Returns the byte offset of this instruction within its function's code section.
     *
     * @return the address (offset).
     */
    public int address() {
        return address;
    }

    /**
     * Returns the {@link OpCode} of this instruction.
     *
     * @return the opcode.
     */
    public OpCode opcode() {
        return opcode;
    }

    /**
     * Returns a copy of the immediate operands for this instruction.
     * Returns {@link #EMPTY_OPERANDS} if there are no operands.
     * A clone is returned to prevent modification of the internal array.
     *
     * @return a copy of the operands array.
     */
    public long[] operands() {
        return operands.clone();
    }

    /**
     * Returns the number of immediate operands for this instruction.
     *
     * @return the operand count.
     */
    public int operandCount() {
        return operands.length;
    }

    /**
     * Returns the immediate operand at the specified index.
     *
     * @param index the index of the operand to retrieve.
     * @return the operand value at the given index.
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds.
     */
    public long operand(int index) {
        return operands[index];
    }

    /**
     * Returns a string representation of this instruction, including its address, opcode, and operands.
     *
     * @return a string representation of the instruction.
     */
    @Override
    public String toString() {
        var result = String.format("0x%08X", address) + ": ";
        if (operands.length > 0) {
            return result + opcode + " " + Arrays.toString(operands);
        }
        return result + opcode.toString();
    }

    /**
     * Compares this instruction to another object for equality.
     * Two instructions are equal if they have the same address, opcode, and operands.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is an {@code Instruction} with the same address, opcode, and operands, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this instruction.
     * The hash code is based on the address, opcode, and operands.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(address, opcode, Arrays.hashCode(operands));
    }
}
