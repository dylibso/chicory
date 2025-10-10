package com.dylibso.chicory.wasm.types;

import java.util.Arrays;
import java.util.Objects;

public class RecType {
    private final SubType[] subTypes;

    // TODO: builders
    public RecType(SubType[] subTypes) {
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
}
