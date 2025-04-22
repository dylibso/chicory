package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the different types of catch clauses used in the
 * `try_table` instruction of the WebAssembly Exception Handling proposal.
 */
public enum CatchOpCode {
    /** Catches exceptions with a specific tag, pushing the exception object onto the stack. */
    CATCH(0x00),
    /** Catches exceptions with a specific tag, pushing a reference to the exception object onto the stack. */
    CATCH_REF(0x01),
    /** Catches any exception, discarding the exception object. */
    CATCH_ALL(0x02),
    /** Catches any exception, pushing a reference to the exception object onto the stack. */
    CATCH_ALL_REF(0x03);

    private static final int OP_CODES_SIZE = 4;

    // trick: the enum constructor cannot access its own static fields
    // but can access another class
    private static final class CatchOpCodes {
        private CatchOpCodes() {}

        private static final CatchOpCode[] byOpCode = new CatchOpCode[OP_CODES_SIZE];
    }

    private final int opcode;

    CatchOpCode(int opcode) {
        this.opcode = opcode;
        CatchOpCodes.byOpCode[opcode] = this;
    }

    /**
     * Returns the numeric opcode value for this catch type.
     *
     * @return the opcode integer.
     */
    public int opcode() {
        return opcode;
    }

    /**
     * Retrieves a {@link CatchOpCode} enum constant by its numeric opcode value.
     *
     * @param opcode the numeric opcode.
     * @return the corresponding {@link CatchOpCode}.
     * @throws ArrayIndexOutOfBoundsException if the opcode is invalid.
     */
    public static CatchOpCode byOpCode(int opcode) {
        // TODO: Add bounds check? Or assume valid input from parser?
        return CatchOpCodes.byOpCode[opcode];
    }

    /**
     * Represents a single decoded catch clause from a `try_table` instruction.
     * Contains the type of catch, the optional tag index, and the target label.
     */
    public static final class Catch {
        private final CatchOpCode opcode;
        private final int tag;
        private final int label;

        private Catch(CatchOpCode opcode, int label) {
            this(opcode, -1, label);
            assert (opcode == CATCH_ALL || opcode == CATCH_ALL_REF);
        }

        private Catch(CatchOpCode opcode, int tag, int label) {
            assert (tag == -1 || opcode == CATCH || opcode == CATCH_REF);
            this.opcode = opcode;
            this.tag = tag;
            this.label = label;
        }

        /**
         * Returns the type of this catch clause.
         *
         * @return the {@link CatchOpCode} enum constant.
         */
        public CatchOpCode opcode() {
            return opcode;
        }

        /**
         * Returns the tag index associated with this catch clause.
         * Applicable only for {@link CatchOpCode#CATCH} and {@link CatchOpCode#CATCH_REF}.
         *
         * @return the tag index, or -1 if not applicable.
         */
        public int tag() {
            return tag;
        }

        /**
         * Returns the target label (instruction index) for this catch clause.
         * This indicates where execution should jump if this clause handles the exception.
         *
         * @return the target instruction index.
         */
        public int label() {
            return label;
        }
    }

    /**
     * Decodes the operands of a `try_table` instruction into a list of {@link Catch} objects.
     * The operands array is expected to follow the structure defined in the Exception Handling proposal:
     * operands[0] = block type (ignored here, handled by Instruction parsing)
     * operands[1] = number of catch clauses (N)
     * operands[2...M] = encoded catch clauses (opcode, [tag], label)
     *
     * @param operands the raw operands array from the {@link Instruction}, starting from the catch count.
     * @return a List of decoded {@link Catch} clauses.
     */
    @SuppressWarnings("ModifiedControlVariable")
    public static List<Catch> decode(long[] operands) {
        var length = operands[1];
        var result = new ArrayList<Catch>();
        for (int i = 2; i < operands.length; i++) {
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    {
                        var tag = (int) operands[i++];
                        var label = (int) operands[i];
                        result.add(new Catch(catchEnum, tag, label));
                        break;
                    }
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    {
                        var label = (int) operands[i];
                        result.add(new Catch(catchEnum, label));
                        break;
                    }
            }
        }
        assert (result.size() == length);
        return result;
    }

    /**
     * Extracts all target labels from the operands of a `try_table` instruction.
     * This is useful for control flow analysis to find all possible jump targets from the try_table.
     *
     * @param operands the raw operands array from the {@link Instruction}.
     * @return a List of target instruction indices for all catch clauses.
     */
    public static List<Integer> allLabels(long[] operands) {
        return decode(operands).stream().map(Catch::label).collect(Collectors.toList());
    }
}
