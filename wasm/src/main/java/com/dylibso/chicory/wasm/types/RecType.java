package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class RecType {
    private final SubType[] subTypes;

    private RecType(SubType[] subTypes) {
        this.subTypes = subTypes.clone();
    }

    public SubType[] subTypes() {
        return subTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecType recType = (RecType) o;
        return Objects.deepEquals(subTypes, recType.subTypes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(subTypes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SubType.Builder[] subTypeBuilders;

        private Builder() {}

        public Builder withSubTypeBuilders(SubType.Builder[] subTypeBuilders) {
            this.subTypeBuilders = subTypeBuilders;
            return this;
        }

        public boolean needsSubstitution() {
            for (int i = 0; i < subTypeBuilders.length; i++) {
                if (subTypeBuilders[i].needsSubstitution()) {
                    return true;
                }
            }
            return false;
        }

        public RecType build(Function<Integer, RecType> context) {
            SubType[] subTypes = new SubType[subTypeBuilders.length];
            for (int i = 0; i < subTypes.length; i++) {
                var builder = subTypeBuilders[i];
                if (!builder.needsSubstitution()) {
                    subTypes[i] = builder.build(j -> null);
                }
            }
            for (int i = 0; i < subTypes.length; i++) {
                var builder = subTypeBuilders[i];
                if (subTypes[i] == null) {
                    subTypes[i] = builder.build(context);
                }
            }

            return new RecType(subTypes);
        }
    }
}
