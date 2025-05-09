package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.InvalidException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*
 * An instruction with additional metadata for control flow.
 */
public final class AnnotatedInstruction extends Instruction {
    public static final int UNDEFINED_LABEL = -1;

    // metadata fields
    private final int depth;
    private final int labelTrue;
    private final int labelFalse;
    private final List<Integer> labelTable;
    private final Instruction scope;

    private AnnotatedInstruction(
            int address,
            OpCode opcode,
            long[] operands,
            int depth,
            int labelTrue,
            int labelFalse,
            List<Integer> labelTable,
            Instruction scope) {
        super(address, opcode, operands);
        this.depth = depth;
        this.labelTrue = labelTrue;
        this.labelFalse = labelFalse;
        this.labelTable = labelTable;
        this.scope = scope;
    }

    public int labelTrue() {
        return labelTrue;
    }

    public int labelFalse() {
        return labelFalse;
    }

    public List<Integer> labelTable() {
        return labelTable;
    }

    public int depth() {
        return depth;
    }

    public Instruction scope() {
        return scope;
    }

    @Override
    public String toString() {
        return "AnnotatedInstruction{"
                + "instruction="
                + super.toString()
                + ", depth="
                + depth
                + ", labelTrue="
                + labelTrue
                + ", labelFalse="
                + labelFalse
                + ", labelTable="
                + labelTable
                + ", scope="
                + scope
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Instruction base;
        private int depth;
        private Optional<Integer> labelTrue = Optional.empty();
        private Optional<Integer> labelFalse = Optional.empty();
        private Optional<List<Integer>> labelTable = Optional.empty();
        private Optional<Instruction> scope = Optional.empty();

        private Builder() {}

        public OpCode opcode() {
            return base.opcode();
        }

        public Optional<Instruction> scope() {
            return scope;
        }

        public Builder from(Instruction ins) {
            this.base = ins;
            return this;
        }

        public Builder withDepth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder withLabelTrue(int label) {
            this.labelTrue = Optional.of(label);
            return this;
        }

        public Builder withLabelFalse(int label) {
            this.labelFalse = Optional.of(label);
            return this;
        }

        public Builder updateLabelFalse(int label) {
            if (this.labelFalse.equals(this.labelTrue)) {
                this.labelFalse = Optional.of(label);
            }
            return this;
        }

        public Builder withLabelTable(List<Integer> labelTable) {
            this.labelTable = Optional.of(labelTable);
            return this;
        }

        public Builder withScope(Instruction scope) {
            this.scope = Optional.of(scope);
            return this;
        }

        public AnnotatedInstruction build() {
            switch (base.opcode()) {
                case BLOCK:
                case LOOP:
                case END:
                case IF:
                case TRY_TABLE:
                    assert (scope.isPresent());
                    break;
                default:
                    assert (scope.isEmpty());
                    break;
            }
            switch (base.opcode()) {
                case BR_IF:
                case BR_ON_NULL:
                case BR_ON_NON_NULL:
                case IF:
                    if (labelFalse.isEmpty()) {
                        throw new InvalidException("unknown label " + base);
                    }
                    // fallthrough
                case ELSE:
                case BR:
                    if (labelTrue.isEmpty()) {
                        throw new InvalidException("unknown label " + base);
                    }
                    break;
                default:
                    assert (labelTrue.isEmpty());
                    assert (labelFalse.isEmpty());
                    break;
            }
            switch (base.opcode()) {
                case BR_TABLE:
                case TRY_TABLE:
                    if (labelTable.isEmpty()) {
                        throw new InvalidException("unknown label table" + base);
                    }
                    break;
                default:
                    assert (labelTable.isEmpty());
                    break;
            }

            return new AnnotatedInstruction(
                    base.address(),
                    base.opcode(),
                    base.operands(),
                    depth,
                    labelTrue.orElse(UNDEFINED_LABEL),
                    labelFalse.orElse(UNDEFINED_LABEL),
                    labelTable.orElse(List.of()),
                    scope.orElse(null));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AnnotatedInstruction)) {
            return false;
        }
        AnnotatedInstruction that = (AnnotatedInstruction) o;
        return depth == that.depth
                && labelTrue == that.labelTrue
                && labelFalse == that.labelFalse
                && Objects.equals(labelTable, that.labelTable)
                && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depth, labelTrue, labelFalse, labelTable, scope);
    }
}
