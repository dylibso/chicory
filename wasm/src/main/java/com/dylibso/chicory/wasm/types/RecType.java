package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

public final class RecType {
    private final SubType[] subTypes;

    private RecType(SubType[] subTypes) {
        this.subTypes = subTypes.clone();
    }

    public SubType[] subTypes() {
        return subTypes;
    }

    public boolean isLegacy() {
        return subTypes.length == 1
                && subTypes[0].compType() != null
                && subTypes[0].compType() != null;
    }

    public FunctionType legacy() {
        assert subTypes.length == 1;
        return subTypes[0].compType().funcType();
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
        private SubType[] subTypes;

        private Builder() {}

        public Builder withSubTypes(SubType[] subTypes) {
            this.subTypes = subTypes;
            return this;
        }

        public RecType build() {
            return new RecType(subTypes);
        }
    }
}
