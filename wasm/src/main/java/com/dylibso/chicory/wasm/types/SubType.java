package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class SubType {
    private final int[] typeIdx;
    private final CompType compType;
    private final boolean isFinal;

    private SubType(int[] typeIdx, CompType compType, boolean isFinal) {
        this.typeIdx = typeIdx.clone();
        this.compType = compType;
        this.isFinal = isFinal;
    }

    public int[] typeIdx() {
        return typeIdx;
    }

    public CompType compType() {
        return compType;
    }

    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubType subType = (SubType) o;
        return Objects.deepEquals(typeIdx, subType.typeIdx)
                && Objects.equals(compType, subType.compType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(typeIdx), compType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int[] typeIdx;
        private CompType.Builder compTypeBuilder;
        private boolean isFinal;

        private Builder() {}

        public Builder withTypeIdx(int[] typeIdx) {
            this.typeIdx = typeIdx;
            return this;
        }

        public Builder withCompTypeBuilder(CompType.Builder compTypeBuilder) {
            this.compTypeBuilder = compTypeBuilder;
            return this;
        }

        public Builder withFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public SubType build(Function<Integer, RecType> context) {
            return new SubType(typeIdx, compTypeBuilder.build(context), isFinal);
        }
    }
}
