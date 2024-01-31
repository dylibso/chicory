package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionSection extends Section {
    private final int[] typeIndices;

    private FunctionSection(int[] typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = typeIndices;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(FunctionSection functionSection) {
        return new Builder(functionSection);
    }

    public int functionCount() {
        return typeIndices.length;
    }

    public int getFunctionType(int idx) {
        Objects.checkIndex(idx, typeIndices.length);
        return typeIndices[idx];
    }

    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        return typeSection.getType(getFunctionType(idx));
    }

    public static final class Builder {

        private ArrayList<Integer> typeIndices;

        private Builder() {
            this.typeIndices = new ArrayList<>();
        }

        private Builder(FunctionSection functionSection) {
            this.typeIndices = new ArrayList<>();
            this.typeIndices.addAll(
                    Arrays.stream(functionSection.typeIndices)
                            .boxed()
                            .collect(Collectors.toList()));
        }

        public Builder addFunctionType(int typeIndex) {
            this.typeIndices.add(typeIndex);
            return this;
        }

        public FunctionSection build() {
            return new FunctionSection(typeIndices.stream().mapToInt(x -> x).toArray());
        }
    }
}
